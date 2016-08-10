package controllers

import java.net.URLDecoder
import java.util

import actions.CommonActions._
import com.gu.i18n.{CountryGroup, Currency, GBP}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Controller, Result}

import scala.concurrent.Future
import utils.Formatters.countryGroupFormatter
import play.api.data.format.Formats._
import services.{PaymentServices}

class PaypalController(ws: WSClient, paymentServices: PaymentServices) extends Controller {
  def executePayment(countryGroup: CountryGroup, paymentId: String, token: String, payerId: String) = NoCacheAction { implicit request =>
    val paypalService = paymentServices.paypalServiceFor(request)
    paypalService.executePayment(paymentId, token, payerId) match {
      case Right(_) => Redirect(routes.Giraffe.thanks(countryGroup).url, SEE_OTHER)
      //TODO show proper error message
      case Left(error) => Ok(s"payment did not work: $error")
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
      //TODO redirect to some error page here
      formWithErrors => BadRequest(JsArray(formWithErrors.errors.map(k => JsString(k.key)))),
      form => {
        val paypalService = paymentServices.paypalServiceFor(request)
        val authResponse = paypalService.getAuthUrl(form.amount, form.countryGroup, form.transactionId)
        authResponse match {
          case Right(url) => Redirect(url, SEE_OTHER)
          //TODO see what to show on error
          case Left(error) => Ok(s"errorMsg:$error")
        }
      }
    )
  }

  //val PaymentError(error) = OK("PAYMENT ERROR PAGE HERE!")


  //TODO this should be somewhere else ( maybe a lambda?)
  def onWebhookEvent = NoCacheAction { implicit request =>
    println(s"HOOK RECEIVED:\n ${request.body}\n\n")

    request.body.asJson.map { json =>
      val eventType = (json \ "event_type")
      val customData = (json \ "resource" \ "custom")

      println(s"event type is $eventType, custom data is $customData")
      Ok("")
    }.getOrElse(BadRequest(""))
  }

  //FOR IPN TODO remove if we use webhooks instead
  //  def onEvent() = NoCacheAction.async { implicit request =>
  //    val sandboxUrl = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_notify-validate"
  //
  //    def verify(formData: Map[String, Seq[String]]): Future[Boolean] = ws.url(sandboxUrl).withHeaders(("Content-Type", "application/x-www-form-urlencoded")).post(formData).map(_.body == "VERIFIED")
  //
  //    //TODO GET HERE HORRIBLE
  //    val form: Map[String, Seq[String]] = request.body.asFormUrlEncoded.get
  //
  //    println(s"IPN RECEIVED:\n ${form.mkString(";")}")
  //    verify(form).map { valid =>
  //      if (valid) {
  //        println("async validation")
  //        val paymentStatus = form.getOrElse("payment_status", Nil).headOption
  //        val customData = form.getOrElse("custom", Nil).headOption
  //        println(s"paypal payment status $paymentStatus") //sometimes it's pending sometimes it's complete not sure what this means
  //        println(s"paypal custom data: ${URLDecoder.decode(customData.getOrElse(""), "UTF-8")}")
  //      }
  //      //else log a warning or something maybe?
  //      Ok("")
  //    }
  //  }
}
