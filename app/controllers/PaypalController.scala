package controllers

import actions.CommonActions._
import cats.data.EitherT
import cats.instances.future._
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import com.gu.i18n.{CountryGroup, Currency}
import com.netaporter.uri.Uri
import com.paypal.api.payments.Payment
import models.SavedContributionData
import models.{ContributionId, IdentityId, PaypalHook}
import play.api.libs.ws.WSClient
import play.api.mvc.{BodyParsers, Controller, Result}
import services.PaymentServices
import play.api.Logger
import play.api.data.Form
import views.support.Test
import utils.MaxAmount
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data.Forms._
import play.filters.csrf.CSRFCheck
import utils.AppError
import utils.PaypalPaymentError
import utils.StoreMetaDataError

import scala.concurrent.{ExecutionContext, Future}

class PaypalController(ws: WSClient, paymentServices: PaymentServices, checkToken: CSRFCheck)(implicit ec: ExecutionContext)
  extends Controller with Redirect {
  import ContribTimestampCookieAttributes._

  def executePayment(
    countryGroup: CountryGroup,
    paymentId: String,
    token: String,
    payerId: String,
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String]
  ) = NoCacheAction.async { implicit request =>

    val paypalService = paymentServices.paypalServiceFor(request)

    def storeMetaData(payment: Payment): EitherT[Future, AppError, (Payment, SavedContributionData)] = {
      val mvtId = Test.testIdFor(request)
      val variant = Test.getContributePageVariant(countryGroup, mvtId, request)
      val idUser = IdentityId.fromRequest(request)

      paypalService.storeMetaData(paymentId, Seq(variant), cmp, intCmp, refererPageviewId, refererUrl, ophanPageviewId, ophanBrowserId, idUser)
        .leftMap[AppError](StoreMetaDataError) // not necessary if storeMetaData() returns a custom error type
        .map(data => (payment, data))
    }

    def notOkResult(error: AppError): Result = error match {
      case PaypalPaymentError(message) =>
        handleError(countryGroup, s"Error executing PayPal payment: $message")
      case _ =>
        val thanksUrl = routes.Contributions.thanks(countryGroup).url
        redirectWithCampaignCodes(thanksUrl)
    }

    def okResult(data: (Payment, SavedContributionData)): Result = data match {
      case (payment, savedData) =>
        redirectWithCampaignCodes(routes.Contributions.postPayment(countryGroup).url)
          .withSession(request.session + ("email" -> savedData.contributor.email))
          .setCookie[ContribTimestampCookieAttributes](payment.getCreateTime)
    }

    paypalService.executePayment(paymentId, payerId)
      .leftMap[AppError](PaypalPaymentError) // not necessary if executePayment() returns a custom error type
      .flatMap(payment => storeMetaData(payment))
      .fold(notOkResult, okResult)
  }

  case class AuthRequest(
    countryGroup: CountryGroup,
    amount: BigDecimal,
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String]
  )

  object AuthRequest {
    implicit val authRequestReads: Reads[AuthRequest] = (
      (__ \ "countryGroup").read[CountryGroup] and
        (__ \ "amount").read(min[BigDecimal](1)) and
        (__ \ "cmp").readNullable[String] and
        (__ \ "intCmp").readNullable[String] and
        (__ \ "refererPageviewId").readNullable[String] and
        (__ \ "refererUrl").readNullable[String] and
        (__ \ "ophanPageviewId").readNullable[String] and
        (__ \ "ophanBrowserId").readNullable[String]
      ) (AuthRequest.apply _)
  }

  case class AuthResponse(approvalUrl:Uri)

  implicit val UriWrites = new Writes[Uri] {
    override def writes(uri: Uri): JsValue = JsString(uri.toString)
  }

  implicit val AuthResponseWrites = Json.writes[AuthResponse]

  implicit val CountryGroupReads = new Reads[CountryGroup] {
    override def reads(json: JsValue): JsResult[CountryGroup] = json match {
      case JsString(id) => CountryGroup.byId(id).map(JsSuccess(_)).getOrElse(JsError("invalid CountryGroup id"))
      case _ => JsError("invalid value for country group")
    }
  }

  private def capAmount(amount: BigDecimal, currency: Currency): BigDecimal = amount min MaxAmount.forCurrency(currency)

  def authorize = checkToken {
    NoCacheAction.async(parse.json[AuthRequest]) { implicit request =>
      val authRequest = request.body
      val amount = capAmount(authRequest.amount, authRequest.countryGroup.currency)
      val paypalService = paymentServices.paypalServiceFor(request)
      val authResponse = paypalService.getAuthUrl(
        amount = amount,
        countryGroup = authRequest.countryGroup,
        contributionId = ContributionId.random,
        cmp = authRequest.cmp,
        intCmp = authRequest.intCmp,
        refererPageviewId = authRequest.refererPageviewId,
        refererUrl = authRequest.refererUrl,
        ophanPageviewId = authRequest.ophanPageviewId,
        ophanBrowserId = authRequest.ophanBrowserId
      )
      authResponse.value map {
        case Right(url) => Ok(Json.toJson(AuthResponse(url)))
        case Left(error) =>
          Logger.error(s"Error getting PayPal auth url: $error")
          InternalServerError("Error getting PayPal auth url")
      }
    }
  }

  def handleError(countryGroup: CountryGroup, error: String) = {
    Logger.error(error)
    Redirect(routes.Contributions.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
  }

  def hook = NoCacheAction.async(BodyParsers.parse.tolerantText) { request =>
    val bodyText = request.body
    val bodyJson = Json.parse(request.body)

    val paypalService = paymentServices.paypalServiceFor(request)
    val validHook = paypalService.validateEvent(request.headers.toSimpleMap, bodyText)

    def withParsedPaypalHook(paypalHookJson: JsValue)(block: PaypalHook => Future[Result]): Future[Result] = {
      bodyJson.validate[PaypalHook] match {
        case JsSuccess(paypalHook, _) if validHook =>
          Logger.info(s"Received paymentHook: ${paypalHook.paymentId}")
          block(paypalHook)
        case JsError(errors) =>
          Logger.error(s"Unable to parse Json, parsing errors: $errors")
          Future.successful(InternalServerError("Unable to parse json payload"))
        case _ =>
          Logger.error(s"A webhook request wasn't valid: $request, headers: ${request.headers.toSimpleMap},body: $bodyText")
          Future.successful(Forbidden("Request isn't signed by Paypal"))
      }
    }

    withParsedPaypalHook(bodyJson) { paypalHook =>
      paypalService.processPaymentHook(paypalHook).value.map {
        case Right(_) => Ok
        case Left(_) => InternalServerError
      }
    }
  }
  case class MetadataUpdate(marketingOptIn: Boolean)

  val metadataUpdateForm: Form[MetadataUpdate] = Form(
    mapping(
      "marketingOptIn"->boolean
    )(MetadataUpdate.apply)(MetadataUpdate.unapply)
  )

  def updateMetadata(countryGroup: CountryGroup) = NoCacheAction.async(parse.form(metadataUpdateForm)) {
    implicit request =>
      val paypalService = paymentServices.paypalServiceFor(request)
      val marketingOptIn = request.body.marketingOptIn
      val idUser = IdentityId.fromRequest(request)
      val contributor = request.session.data.get("email") match {
        case Some(email) => paypalService.updateMarketingOptIn(email, marketingOptIn, idUser).value
        case None => Future.successful(Logger.error("email not found in session while trying to update marketing opt in"))
      }
      contributor.map { _ =>
        Redirect(routes.Contributions.thanks(countryGroup).url, SEE_OTHER)
      }
  }
}
