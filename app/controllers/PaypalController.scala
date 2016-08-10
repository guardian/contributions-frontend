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


//TODO THIS CODE IS HORRIBLE (option.get synchronous posts and stuff like that, clean up later!!!)
class PaypalController(ws: WSClient) extends Controller {
//TODO this is duplicated with the code in GIraffe, REfactor to a shared location
  implicit val currencyFormatter = new Formatter[Currency] {
    type Result = Either[Seq[FormError], Currency]
    override def bind(key: String, data: Map[String, String]): Result =
      data.get(key).map(_.toUpperCase).flatMap(Currency.fromString).fold[Result](Left(Seq.empty))(currency => Right(currency))
    override def unbind(key: String, value: Currency): Map[String, String] =
      Map(key -> value.identifier)
  }
  //sandbox credentials TODO put this somewhere safe!

  val clientId = "AZE-dMdjnoHspCAYmbuH5nKO72P9gk1dhZEh0CGRU3kOAWsYkBxBm_3ww-vrOawc4FjKH1MkFJ3i1aPv"
  val clientSecret = "EIvEsZPgpNNatteS1OGDajW9WnWpiJmAVfKN-Ne-UHy9-4HAViFRvos2jgnXp_MmWW9aITkEAvYz0Vgu"
  def apiContext: APIContext = new APIContext(clientId, clientSecret, "sandbox");


  val sandboxUrl = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_notify-validate"

  //FOR IPN
  def onEvent() = NoCacheAction.async { implicit request =>
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

  def onWebhookEvent = NoCacheAction { implicit request =>
    println(s"HOOK RECEIVED:\n ${request.body}\n\n")

    request.body.asJson.map { json =>
      val eventType = (json \ "event_type")
      val customData = (json \ "resource" \ "custom")

      println(s"event type is $eventType, custom data is $customData")
      Ok("")
    }.getOrElse(BadRequest(""))
  }

  def verify(formData: Map[String, Seq[String]]): Future[Boolean] = ws.url(sandboxUrl).withHeaders(("Content-Type", "application/x-www-form-urlencoded")).post(formData).map(_.body == "VERIFIED")

  case class PaymentData(
    currency: com.gu.i18n.Currency,
    amount: BigDecimal
  )

  val paypalForm = Form(
    mapping(
      "currency_code" -> of[com.gu.i18n.Currency],
      "amount" -> bigDecimal(10, 2)
    )(PaymentData.apply)(PaymentData.unapply)
  )

///TODO maybe make this easier to understand?
  def authorize = NoCacheAction.async { implicit request =>
    paypalForm.bindFromRequest().fold[Future[Result]]({ withErrors => Future.successful(BadRequest(JsArray(withErrors.errors.map(k => JsString(k.key)))))
    }, { f => Future.successful(authPaypal(f.amount, f.currency))
    })
  }




  def executePayment(paymentId: String, token: String, payerId: String) = NoCacheAction { implicit request =>
   // Redirect(routes.Giraffe.thanks(CountryGroup.UK).url, SEE_OTHER)
    val payment = new Payment();
    payment.setId(paymentId);
    val paymentExecution = new PaymentExecution();
    paymentExecution.setPayerId(payerId);
    try {

      val createdPayment = payment.execute(apiContext, paymentExecution);
      //TODO GET THE REAL COUNTRY GROUP HERE
      Redirect(routes.Giraffe.thanks(CountryGroup.UK).url, SEE_OTHER)
    } catch {
      case e: PayPalRESTException => Ok(s"payment did not work ${e.getMessage}")
    }
  }
  private def authPaypal(amount:BigDecimal, currency: com.gu.i18n.Currency):Result = {

    val paypalAmount = new Amount()

    val currencyCode = currency.toString
    paypalAmount.setCurrency(currencyCode)
    paypalAmount.setTotal(amount.toString)

    val transaction = new Transaction()
    transaction.setAmount(paypalAmount)
    transaction.setDescription("Contribution to the guardian.")
    transaction.setCustom("some custom data here")

    val item = new Item()
    item.setDescription("Contribution to the guardian")
    item.setCurrency(currencyCode)
    item.setPrice(amount.toString())
    item.setQuantity("1")
    val itemList = new ItemList()
    itemList.setItems(List(item).asJava)

    transaction.setItemList(itemList)

    val transactions = new util.ArrayList[Transaction]()
    transactions.add(transaction)

    val payer = new Payer()
    payer.setPaymentMethod("paypal")
    val payment = new Payment()

    payment.setIntent("sale")
    payment.setPayer(payer)
    payment.setTransactions(transactions)

    // ###Redirect URLs
    val redirectUrls = new RedirectUrls()
    redirectUrls.setCancelUrl("https://6802b97f.ngrok.io")
    redirectUrls.setReturnUrl("https://6802b97f.ngrok.io/paypal/execute")

    payment.setRedirectUrls(redirectUrls)
    // Create a payment by posting to the APIService
    // using a valid AccessToken
    // The return object contains the status;
    try {
      val createdPayment: Payment = payment.create(apiContext)
      val links = createdPayment.getLinks.asScala
      val approvalLink = links.find(_.getRel.equalsIgnoreCase("approval_url"))
      //TODO see what to return when we dont have an approval_url in the response
      approvalLink.map(link => Redirect(link.getHref(), SEE_OTHER)).getOrElse(Ok(""))
    }

    catch {
      case e: PayPalRESTException => println("Exception! Payment with PayPal" + e.getMessage());
        //TODO see what to do in this case

        Ok("")
    }

  }

}
