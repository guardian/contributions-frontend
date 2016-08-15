package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.ws.WSClient
import play.api.mvc.{Controller, Result}
import utils.Formatters.countryGroupFormatter
import play.api.data.format.Formats._
import services.PaymentServices
import play.api.Logger

import scala.util.Right


class PaypalController(ws: WSClient, paymentServices: PaymentServices) extends Controller {
  val PaypalErrorCode = "paypalError"
  def executePayment(countryGroup: CountryGroup, paymentId: String, token: String, payerId: String) = NoCacheAction { implicit request =>
    val paypalService = paymentServices.paypalServiceFor(request)
    paypalService.executePayment(paymentId, token, payerId) match {
      case Right(_) => Redirect(routes.Giraffe.thanks(countryGroup).url, SEE_OTHER)
      case Left(error) => handleError(countryGroup, s"Error executing PayPal payment: $error")
    }
  }

  case class PaymentData(
    countryGroup: CountryGroup,
    amount: BigDecimal,
    transactionId: String
  )

  val paypalForm = Form(
    mapping(
      "countryGroup" -> of[CountryGroup],
      "amount" -> bigDecimal(10, 2),
      "transactionId" -> of[String]
    )(PaymentData.apply)(PaymentData.unapply)
  )

  def authorize = NoCacheAction { implicit request =>
    paypalForm.bindFromRequest().fold[Result](
      hasErrors = form => handleError(CountryGroup.UK, form.errors.mkString(",")),
      success = form => {
        val paypalService = paymentServices.paypalServiceFor(request)
        val maxAllowedAmount = configuration.Payment.maxAmountFor(form.countryGroup.currency)
        val amount = form.amount.min(maxAllowedAmount)
        val authResponse = paypalService.getAuthUrl(amount, form.countryGroup, form.transactionId)
        authResponse match {
          case Right(url) => Redirect(url, SEE_OTHER)
          case Left(error) => handleError(form.countryGroup, s"Error getting PayPal auth url: $error")
        }
      }
    )
  }

  def handleError(countryGroup: CountryGroup, error: String) = {
    Logger.error(error)
    Redirect(routes.Giraffe.contribute(countryGroup, Some(PaypalErrorCode)).url, SEE_OTHER)
  }
}
