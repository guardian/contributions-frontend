package controllers

import actions.CommonActions._
import cats.data.Xor
import com.gu.i18n.{CountryGroup, Currency}
import com.netaporter.uri.Uri
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

import scala.concurrent.{ExecutionContext, Future}

class PaypalController(
  ws: WSClient,
  paymentServices: PaymentServices
)(
  implicit ec: ExecutionContext
) extends Controller with Redirect {


  def executePayment(
    countryGroup: CountryGroup,
    paymentId: String,
    token: String,
    payerId: String,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  ) = NoCacheAction.async { implicit request =>
    val mvtId = Test.testIdFor(request)

    def thanksUrl = routes.Giraffe.thanks(countryGroup).url
    def postPayUrl = routes.Giraffe.postPayment(countryGroup).url
    val variant = Test.getContributePageVariant(countryGroup, mvtId, request)
    val paypalService = paymentServices.paypalServiceFor(request)
    val idUser = IdentityId.fromRequest(request)

    def saveMetadata = paypalService.storeMetaData(paymentId, Seq(variant), cmp, intCmp, ophanId, idUser).value.map {
      case Xor.Right(savedData) => redirectWithCampaignCodes(postPayUrl).withSession(request.session + ("email" -> savedData.contributor.email))
      case Xor.Left(_) => redirectWithCampaignCodes(thanksUrl)
    }

    paypalService.executePayment(paymentId, payerId).value flatMap {
      case Xor.Right(_) => saveMetadata
      case Xor.Left(error) => Future.successful(handleError(countryGroup, s"Error executing PayPal payment: $error"))
    }

  }

  case class AuthRequest(
    countryGroup: CountryGroup,
    amount: BigDecimal,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  )

  object AuthRequest {
    implicit val authRequestReads: Reads[AuthRequest] = (
      (__ \ "countryGroup").read[CountryGroup] and
        (__ \ "amount").read(min[BigDecimal](1)) and
        (__ \ "cmp").readNullable[String] and
        (__ \ "intCmp").readNullable[String] and
        (__ \ "ophanId").readNullable[String]
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

  def authorize = NoCacheAction.async(parse.json) { request =>
    request.body.validate[AuthRequest] match {
      case JsSuccess(authRequest, _) =>
        val paypalService = paymentServices.paypalServiceFor(request)
        val authResponse = paypalService.getAuthUrl(
          amount = capAmount(authRequest.amount, authRequest.countryGroup.currency),
          countryGroup = authRequest.countryGroup,
          contributionId = ContributionId.random,
          cmp = authRequest.cmp,
          intCmp = authRequest.intCmp,
          ophanId = authRequest.ophanId
        )
        authResponse.value map {
          case Xor.Right(url) => Ok(Json.toJson(AuthResponse(url)))
          case Xor.Left(error) =>
            Logger.error(s"Error getting PayPal auth url: $error")
            InternalServerError("Error getting PayPal auth url")
        }
      case JsError(error) =>
        Logger.error(s"Invalid request=$error")
        Future.successful(BadRequest(s"Invalid request=$error"))
    }
  }

  def handleError(countryGroup: CountryGroup, error: String) = {
    Logger.error(error)
    Redirect(routes.Giraffe.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
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
        case Xor.Right(_) => Ok
        case Xor.Left(_) => InternalServerError
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
      val contributor = request.session.data.get("email") match {
        case Some(email) => paypalService.updateMarketingOptIn(email, request.body.marketingOptIn).value
        case None => Future.successful(Logger.error("email not found in session while trying to update marketing opt in"))
      }
      contributor.map { _ =>
        Redirect(routes.Giraffe.thanks(countryGroup).url, SEE_OTHER)
      }
  }

}
