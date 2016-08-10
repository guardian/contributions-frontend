package controllers

import java.net.URLDecoder
import java.util

import actions.CommonActions._
import com.gu.i18n.{CountryGroup, Currency, GBP}
import com.paypal.api.payments._
import com.paypal.base.rest.{APIContext, PayPalRESTException}
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.BodyParsers.parse.{json => BodyJson}
import play.api.mvc.{Controller, Result}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import utils.Formatters.currencyFormatter
import play.api.data.format.Formats._

//TODO THIS CODE IS HORRIBLE (option.get synchronous posts and stuff like that, clean up later!!!)
class PaypalController(ws: WSClient) extends Controller {

  //sandbox credentials TODO put this somewhere safe!

  val clientId = "AZE-dMdjnoHspCAYmbuH5nKO72P9gk1dhZEh0CGRU3kOAWsYkBxBm_3ww-vrOawc4FjKH1MkFJ3i1aPv"
  val clientSecret = "EIvEsZPgpNNatteS1OGDajW9WnWpiJmAVfKN-Ne-UHy9-4HAViFRvos2jgnXp_MmWW9aITkEAvYz0Vgu"
  def apiContext: APIContext = new APIContext(clientId, clientSecret, "sandbox");


  val sandboxUrl = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_notify-validate"

  //FOR IPN TODO remove if we use webhooks instead
  def onEvent() = NoCacheAction.async { implicit request =>
    def verify(formData: Map[String, Seq[String]]): Future[Boolean] = ws.url(sandboxUrl).withHeaders(("Content-Type", "application/x-www-form-urlencoded")).post(formData).map(_.body == "VERIFIED")

    //TODO GET HERE HORRIBLE
    val form: Map[String, Seq[String]] = request.body.asFormUrlEncoded.get

    println(s"IPN RECEIVED:\n ${form.mkString(";")}")
    verify(form).map { valid =>
      if (valid) {
        println("async validation")
        val paymentStatus = form.getOrElse("payment_status", Nil).headOption
        val customData = form.getOrElse("custom", Nil).headOption
        println(s"paypal payment status $paymentStatus") //sometimes it's pending sometimes it's complete not sure what this means
        println(s"paypal custom data: ${URLDecoder.decode(customData.getOrElse(""), "UTF-8")}")
      }
      //else log a warning or something maybe?
      Ok("")
    }
  }
  //TODO see if this should be somewhere else ( maybe a lambda?)
  def onWebhookEvent = NoCacheAction { implicit request =>
    println(s"HOOK RECEIVED:\n ${request.body}\n\n")

    request.body.asJson.map { json =>
      val eventType = (json \ "event_type")
      val customData = (json \ "resource" \ "custom")

      println(s"event type is $eventType, custom data is $customData")
      Ok("")
    }.getOrElse(BadRequest(""))
  }


  case class PaymentData(
    currency: Currency,
    amount: BigDecimal,
    transactionId:String
  )

  val paypalForm = Form(
    mapping(
      "currency_code" -> of[Currency],
      "amount" -> bigDecimal(10, 2),
      "transactionId" ->of[String]
    )(PaymentData.apply)(PaymentData.unapply)
  )

  def authorize = NoCacheAction { implicit request =>
    paypalForm.bindFromRequest().fold[Result](
      //TODO redirect to some error page here
      formWithErrors => BadRequest(JsArray(formWithErrors.errors.map(k => JsString(k.key)))),
      formWithoutErrors => authPaypal(formWithoutErrors.amount, formWithoutErrors.currency, formWithoutErrors.transactionId)
    )
  }

  def executePayment(paymentId: String, token: String, payerId: String) = NoCacheAction { implicit request =>
    val payment = new Payment().setId(paymentId)
    val paymentExecution = new PaymentExecution().setPayerId(payerId)
    try {

      val createdPayment = payment.execute(apiContext, paymentExecution);
      //TODO GET THE REAL COUNTRY GROUP HERE
      Redirect(routes.Giraffe.thanks(CountryGroup.UK).url, SEE_OTHER)
    } catch {
      case e: PayPalRESTException => Ok(s"payment did not work ${e.getMessage}")
    }
  }

  private def authPaypal(amount: BigDecimal, currency: Currency, transactionId: String): Result = {
    val description = "Contribution to the guardian"
    //TODO get from some sort of config (with sandbox and live urls depending on the env)
    val cancelUrl = "https://6802b97f.ngrok.io"
    val returnUrl = "https://6802b97f.ngrok.io/paypal/execute"
    val currencyCode = currency.toString
    val paypalAmount = new Amount().setCurrency(currencyCode).setTotal(amount.toString)
    val item = new Item().setDescription(description).setCurrency(currencyCode).setPrice(amount.toString).setQuantity("1")
    val itemList = new ItemList().setItems(List(item).asJava)
    val transaction = new Transaction
    transaction.setAmount(paypalAmount)
    transaction.setDescription(description)
    transaction.setCustom(transactionId)
    transaction.setItemList(itemList)

    val transactions = new util.ArrayList[Transaction]()
    transactions.add(transaction)

    val payer = new Payer().setPaymentMethod("paypal")
    val redirectUrls = new RedirectUrls().setCancelUrl(cancelUrl).setReturnUrl(returnUrl)

    val payment = new Payment().setIntent("sale").setPayer(payer).setTransactions(transactions).setRedirectUrls(redirectUrls)
    try {
      val createdPayment: Payment = payment.create(apiContext)
      val links = createdPayment.getLinks.asScala
      val approvalLink = links.find(_.getRel.equalsIgnoreCase("approval_url"))
      //TODO see what to return when we dont have an approval_url in the response
      approvalLink.map(link => Redirect(link.getHref, SEE_OTHER)).getOrElse(Ok(""))
    }

    catch {
      case e: PayPalRESTException => println("Exception! Payment with PayPal" + e.getMessage);
        //TODO see what to do in this case
        Ok("")
    }

  }

}
