package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.libs.ws.WSClient
import play.api.mvc.{Controller, Result}
import services.PaymentServices
import play.api.Logger
import play.api.data.format.Formatter
import play.api.libs.json._
import utils.ContributionIdGenerator
import play.api.libs.functional.syntax._

import scala.util.Right


class PaypalController(ws: WSClient, paymentServices: PaymentServices, contributionIdGenerator :ContributionIdGenerator) extends Controller {

  implicit val countryGroupFormatter = new Formatter[CountryGroup] {
    type Result = Either[Seq[FormError], CountryGroup]

    override def bind(key: String, data: Map[String, String]): Result = {
      data.get(key).flatMap(CountryGroup.byId(_)).fold[Result](Left(Seq.empty))(countryGroup => Right(countryGroup))
    }

    override def unbind(key: String, value: CountryGroup): Map[String, String] = Map(key -> value.id)
  }

  def executePayment(countryGroup: CountryGroup, paymentId: String, token: String, payerId: String) = NoCacheAction { implicit request =>
    val paypalService = paymentServices.paypalServiceFor(request)
    paypalService.executePayment(paymentId, token, payerId) match {
      case Right(_) => Redirect(routes.Giraffe.thanks(countryGroup).url, SEE_OTHER)
      case Left(error) => handleError(countryGroup, s"Error executing PayPal payment: $error")
    }
  }

  case class PaymentData(
    countryGroup: CountryGroup,
    amount: BigDecimal
  )

  val paypalForm = Form(
    mapping(
      "countryGroup" -> of[CountryGroup],
      "amount" -> bigDecimal(10, 2)
    )(PaymentData.apply)(PaymentData.unapply)
  )

  def authorize = NoCacheAction { implicit request =>
    paypalForm.bindFromRequest().fold[Result](
      hasErrors = form => handleError(CountryGroup.UK, form.errors.mkString(",")),
      success = form => {
        val paypalService = paymentServices.paypalServiceFor(request)
        val maxAllowedAmount = configuration.Payment.maxAmountFor(form.countryGroup.currency)
        val amount = form.amount.min(maxAllowedAmount)
        val authResponse = paypalService.getAuthUrl(amount, form.countryGroup, contributionIdGenerator.getNewId)
        authResponse match {
          case Right(url) => Redirect(url, SEE_OTHER)
          case Left(error) => handleError(form.countryGroup, s"Error getting PayPal auth url: $error")
        }
      }
    )
  }

  case class AuthRequest(countryGroup: CountryGroup, amount: BigDecimal)
  case class AuthResponse(approvalUrl:String)

  implicit val AuthResponseWrites = Json.writes[AuthResponse]
  implicit val CountryGroupReads = new Reads[CountryGroup] {
    override def reads(json: JsValue): JsResult[CountryGroup] = json match {
      case JsString(id) => CountryGroup.byId(id).map(JsSuccess(_)).getOrElse(JsError("invalid countrygroup id"))
      case _ => JsError("invalid value for country group")
    }

  }
  implicit val AuthRequestReads: Reads[AuthRequest] = (
    (JsPath \ "countryGroup").read[CountryGroup] and
      (JsPath \ "amount").read[BigDecimal]
    ) (AuthRequest.apply _)

  def ajaxAuth = NoCacheAction(parse.json) { request =>
    request.body.validate[AuthRequest] match {
      case JsSuccess(authRequest, _) =>
        val paypalService = paymentServices.paypalServiceFor(request)
        val authResponse = paypalService.getAuthUrl(authRequest.amount, authRequest.countryGroup, contributionIdGenerator.getNewId)
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
}
