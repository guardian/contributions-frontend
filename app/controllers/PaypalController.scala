package controllers

import actions.CommonActions._
import com.gu.i18n.{CountryGroup, Currency}
import models.PaymentHook
import play.api.libs.ws.WSClient
import play.api.mvc.{BodyParsers, Controller, Result}
import services.PaymentServices
import play.api.Logger
import play.api.libs.json._
import utils.ContributionIdGenerator
import views.support.Test
import utils.MaxAmount
import scala.util.Right


class PaypalController(
  ws: WSClient,
  paymentServices: PaymentServices
) extends Controller {

  def executePayment(
    countryGroup: CountryGroup,
    paymentId: String,
    token: String,
    payerId: String,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  ) = NoCacheAction { implicit request =>
    val chosenVariants = Test.getContributePageVariants(countryGroup, request)
    val paypalService = paymentServices.paypalServiceFor(request)
    val idUser = IdentityUser.fromRequest(request).map(_.id)
    paypalService.executePayment(paymentId, payerId) match {
      case Right(_) =>
        paypalService.storeMetaData(paymentId, chosenVariants, cmp, intCmp, ophanId, idUser)
        Redirect(routes.Giraffe.thanks(countryGroup).url, SEE_OTHER)
      case Left(error) => handleError(countryGroup, s"Error executing PayPal payment: $error")
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
    implicit val jf = Json.reads[AuthRequest]
  }
  case class AuthResponse(approvalUrl:String)

  implicit val AuthResponseWrites = Json.writes[AuthResponse]

  implicit val CountryGroupReads = new Reads[CountryGroup] {
    override def reads(json: JsValue): JsResult[CountryGroup] = json match {
      case JsString(id) => CountryGroup.byId(id).map(JsSuccess(_)).getOrElse(JsError("invalid CountryGroup id"))
      case _ => JsError("invalid value for country group")
    }
  }

  private def capAmount(amount: BigDecimal, currency: Currency): BigDecimal = amount min MaxAmount.forCurrency(currency)

  def authorize = NoCacheAction(parse.json) { request =>
    request.body.validate[AuthRequest] match {
      case JsSuccess(authRequest, _) =>
        val paypalService = paymentServices.paypalServiceFor(request)
        val authResponse = paypalService.getAuthUrl(
          amount = capAmount(authRequest.amount, authRequest.countryGroup.currency),
          countryGroup = authRequest.countryGroup,
          contributionId = ContributionIdGenerator.getNewId,
          cmp = authRequest.cmp,
          intCmp = authRequest.intCmp,
          ophanId = authRequest.ophanId
        )
        authResponse match {
          case Right(url) => Ok(Json.toJson(AuthResponse(url)))
          case Left(error) => handleError(authRequest.countryGroup, s"Error getting PayPal auth url: $error")
        }
      case JsError(error) =>

        Logger.warn(s"Invalid request=$error")
        BadRequest(s"Invalid request=$error")
    }
  }

  def handleError(countryGroup: CountryGroup, error: String) = {
    Logger.error(error)
    Redirect(routes.Giraffe.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
  }

  def hook = NoCacheAction(BodyParsers.parse.tolerantText) { request =>
    val bodyText = request.body
    val bodyJson = Json.parse(request.body)

    val paypalService = paymentServices.paypalServiceFor(request)
    val validHook = paypalService.validateEvent(request.headers.toSimpleMap, bodyText)

    bodyJson.validate[PaymentHook] match {
      case JsSuccess(paymentHook, _) if validHook =>
        paypalService.processPaymentHook(paymentHook)
        Logger.info(s"Received paymentHook: $paymentHook")
        Ok
      case JsError(errors) =>
        Logger.error(s"Unable to parse Json, parsing errors: $errors")
        InternalServerError("Unable to parse json payload")
      case _ =>
        Logger.error(s"A webhook request wasn't valid: $request, headers: ${request.headers.toSimpleMap},body: $bodyText")
        Forbidden("Request isn't signed by Paypal")
    }
  }
}
