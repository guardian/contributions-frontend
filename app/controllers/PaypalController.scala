package controllers

import actions.CommonActions._
import cats.instances.future._
import cats.syntax.show._
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import com.gu.i18n.{CountryGroup, Currency}
import com.netaporter.uri.Uri
import com.paypal.api.payments.Payment
import models._
import monitoring.TagAwareLogger
import monitoring.LoggingTags
import monitoring.LoggingTagsProvider
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.PaymentServices
import play.api.data.Form
import utils.MaxAmount
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data.Forms._
import play.filters.csrf.CSRFCheck

import scala.concurrent.{ExecutionContext, Future}

class PaypalController(ws: WSClient, paymentServices: PaymentServices, checkToken: CSRFCheck)(implicit ec: ExecutionContext)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {
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
    ophanBrowserId: Option[String],
    ophanVisitId: Option[String]
  ) = (NoCacheAction andThen MobileSupportAction andThen ABTestAction).async { implicit request =>

    val paypalService = paymentServices.paypalServiceFor(request)

    def storeMetaData(payment: Payment) =
      paypalService.storeMetaData(
        paymentId = paymentId,
        testAllocations = request.testAllocations,
        cmp = cmp,
        intCmp = intCmp,
        refererPageviewId = refererPageviewId,
        refererUrl = refererUrl,
        ophanPageviewId = ophanPageviewId,
        ophanBrowserId = ophanBrowserId,
        idUser = IdentityId.fromRequest(request),
        platform = request.platform,
        ophanVisitId = ophanVisitId
      )

    def notOkResult(message: String): Result =
      handleError(countryGroup, s"Error executing PayPal payment: $message")

    def okResult(payment: Payment): Result = {
      val redirectUrl = routes.Contributions.postPayment(countryGroup).url
      val amount = paypalService.paymentAmount(payment)
      val email = payment.getPayer.getPayerInfo.getEmail
      val session = List("email" -> email) ++ amount.map("amount" -> _.show)

      redirectWithCampaignCodes(redirectUrl)
        .addingToSession(session :_ *)
        .setCookie[ContribTimestampCookieAttributes](payment.getCreateTime)
    }

    paypalService.executePayment(paymentId, payerId)
      .map { payment => storeMetaData(payment); payment }
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
    ophanBrowserId: Option[String],
    ophanVisitId: Option[String]
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
        (__ \ "ophanBrowserId").readNullable[String] and
        (__ \ "ophanVisitId").readNullable[String]
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
        ophanBrowserId = authRequest.ophanBrowserId,
        ophanVisitId = authRequest.ophanVisitId
      )
      authResponse.value map {
        case Right(url) => Ok(Json.toJson(AuthResponse(url)))
        case Left(err) =>
          error(s"Error getting PayPal auth url: $err")
          InternalServerError("Error getting PayPal auth url")
      }
    }
  }

  def handleError(countryGroup: CountryGroup, err: String)(implicit tags: LoggingTags) = {
    error(err)
    Redirect(routes.Contributions.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
  }

  def hook = NoCacheAction.async(parse.tolerantText) { implicit request =>
    val bodyText = request.body
    val bodyJson = Json.parse(request.body)

    val paypalService = paymentServices.paypalServiceFor(request)
    val validHook = paypalService.validateEvent(request.headers.toSimpleMap, bodyText)

    def withParsedPaypalHook(paypalHookJson: JsValue)(block: PaypalHook => Future[Result]): Future[Result] = {
      bodyJson.validate[PaypalHook] match {
        case JsSuccess(paypalHook, _) if validHook =>
          info(s"Received paymentHook: ${paypalHook.paymentId}")
          block(paypalHook)
        case JsError(err) =>
          error(s"Unable to parse Json, parsing errors: $err")
          Future.successful(InternalServerError("Unable to parse json payload"))
        case _ =>
          error(s"A webhook request wasn't valid: $request, headers: ${request.headers.toSimpleMap},body: $bodyText")
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

  def updateMetadata(countryGroup: CountryGroup) = NoCacheAction.async(parse.form(metadataUpdateForm)) { implicit request =>
    val paypalService = paymentServices.paypalServiceFor(request)
    val marketingOptIn = request.body.marketingOptIn
    val idUser = IdentityId.fromRequest(request)
    val contributor = request.session.data.get("email") match {
      case Some(email) => paypalService.updateMarketingOptIn(email, marketingOptIn, idUser).value
      case None => Future.successful(error("email not found in session while trying to update marketing opt in"))
    }

    val url = request.session.get("amount").flatMap(ContributionAmount.apply)
      .filter(_ => request.isAndroid)
      .map(mobileRedirectUrl)
      .getOrElse(routes.Contributions.thanks(countryGroup).url)

    contributor.map { _ =>
      Redirect(url, SEE_OTHER)
    }
  }
}
