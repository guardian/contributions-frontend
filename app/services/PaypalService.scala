package services

import com.gu.i18n.CountryGroup
import com.paypal.api.payments._
import com.paypal.base.rest.{APIContext, PayPalRESTException}

import scala.collection.JavaConverters._
import com.typesafe.config.Config

case class PaypalCredentials(clientId: String, clientSecret: String)

case class PaypalApiConfig(envName: String, paypalMode: String, baseReturnUrl: String, credentials: PaypalCredentials)

object PaypalApiConfig {
  def from(config: Config, environmentName: String, variant: String = "api") = PaypalApiConfig(
    envName = environmentName,
    credentials = PaypalCredentials(config.getString("clientId"), config.getString("clientSecret")),
    paypalMode = config.getString("paypalMode"),
    baseReturnUrl = config.getString("baseReturnUrl")
  )
}

class PaypalService(config: PaypalApiConfig) {
  val description = "Contribution to the guardian"
  val credentials = config.credentials


  def apiContext: APIContext = new APIContext(credentials.clientId, credentials.clientSecret, config.paypalMode)

  def getAuthUrl(amount: BigDecimal, countryGroup: CountryGroup, contributionId: String): Either[String, String] = {
    val cancelUrl = config.baseReturnUrl
    val returnUrl = s"${config.baseReturnUrl}/paypal/${countryGroup.id}/execute"
    val currencyCode = countryGroup.currency.toString
    val paypalAmount = new Amount().setCurrency(currencyCode).setTotal(amount.toString)
    val item = new Item().setDescription(description).setCurrency(currencyCode).setPrice(amount.toString).setQuantity("1")
    val itemList = new ItemList().setItems(List(item).asJava)
    val transaction = new Transaction
    transaction.setAmount(paypalAmount)
    transaction.setDescription(description)
    transaction.setCustom(contributionId)
    transaction.setItemList(itemList)

    val transactions = List(transaction).asJava

    val payer = new Payer().setPaymentMethod("paypal")
    val redirectUrls = new RedirectUrls().setCancelUrl(cancelUrl).setReturnUrl(returnUrl)

    val payment = new Payment().setIntent("sale").setPayer(payer).setTransactions(transactions).setRedirectUrls(redirectUrls)
    try {
      val createdPayment: Payment = payment.create(apiContext)
      val links = createdPayment.getLinks.asScala
      val approvalLink = links.find(_.getRel.equalsIgnoreCase("approval_url"))
      approvalLink.map(l => Right(l.getHref)).getOrElse(Left("No approval link returned from paypal"))
    }
    catch {
      case e: PayPalRESTException => Left(e.getMessage)
    }
  }

  def executePayment(paymentId: String, token: String, payerId: String): Either[String, Unit] = {
    val payment = new Payment().setId(paymentId)
    val paymentExecution = new PaymentExecution().setPayerId(payerId)
    try {
      val createdPayment = payment.execute(apiContext, paymentExecution)
      if (createdPayment.getState.toUpperCase != "APPROVED") {
        Left(s"payment returned with state: ${createdPayment.getState}")
      }
      else
        Right(Unit)
    } catch {
      case e: PayPalRESTException => Left(e.getMessage)
    }
  }

}