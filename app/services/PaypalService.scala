package services

import java.util.UUID

import com.gu.i18n.CountryGroup
import com.paypal.api.payments._
import com.paypal.base.Constants
import com.paypal.base.rest.{APIContext, PayPalRESTException}

import scala.collection.JavaConverters._
import com.typesafe.config.Config
import data.ContributionData
import models.ContributionMetaData
import org.joda.time.DateTime
import play.api.Logger
import views.support.ChosenVariants

import scala.util.{Failure, Success, Try}

case class PaypalCredentials(clientId: String, clientSecret: String)

case class PaypalApiConfig(
  envName: String,
  paypalMode: String,
  baseReturnUrl: String,
  credentials: PaypalCredentials,
  paypalWebhookId: String
)

object PaypalApiConfig {
  def from(config: Config, environmentName: String, variant: String = "api") = PaypalApiConfig(
    envName = environmentName,
    credentials = PaypalCredentials(config.getString("clientId"), config.getString("clientSecret")),
    paypalMode = config.getString("paypalMode"),
    baseReturnUrl = config.getString("baseReturnUrl"),
    paypalWebhookId = config.getString("paypalWebhookId")
  )
}

class PaypalService(config: PaypalApiConfig, contributionData: ContributionData) {
  val description = "Contribution to the guardian"
  val credentials = config.credentials


  def apiContext: APIContext = new APIContext(credentials.clientId, credentials.clientSecret, config.paypalMode)

  def getAuthUrl(
    amount: BigDecimal,
    countryGroup: CountryGroup,
    contributionId: String,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  ): Either[String, String] = {

    def returnUrl: String = {
      val extraParams = List(
        cmp.map(value => s"CMP=$value"),
        intCmp.map(value => s"INTCMP=$value"),
        ophanId.map(value => s"ophanId=$value")
      ).flatten match {
        case Nil => ""
        case params => params.mkString("?", "&", "")
      }
      s"${config.baseReturnUrl}/paypal/${countryGroup.id}/execute$extraParams"
    }

    val cancelUrl = config.baseReturnUrl
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
    } catch {
      case e: PayPalRESTException => Left(e.getMessage)
    }
  }

  def executePayment(
    paymentId: String,
    token: String,
    payerId: String,
    chosenVariants: ChosenVariants,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  ): Either[String, String] = {
    def execute(): Either[String, String] = {
      val payment = new Payment().setId(paymentId)
      val paymentExecution = new PaymentExecution().setPayerId(payerId)
      try {
        val createdPayment = payment.execute(apiContext, paymentExecution)
        if (createdPayment.getState.toUpperCase != "APPROVED") {
          Left(s"payment returned with state: ${createdPayment.getState}")
        } else {
          Payment.get(apiContext, paymentId)
          Right(createdPayment.getId)
        }
      } catch {
        case e: PayPalRESTException => Left(e.getMessage)
      }
    }

    def metaData(paymentId: String): Right[String, String] = {
      val result = for {
        payment <- Try(Payment.get(apiContext, paymentId))
        transaction <- Try(payment.getTransactions.asScala.head)
        contributionId <- Try(UUID.fromString(transaction.getCustom))
        created <- Try(new DateTime(payment.getCreateTime))
      } yield {
        val metadata = ContributionMetaData(
          contributionId = contributionId,
          created = created,
          email = payment.getPayer.getPayerInfo.getEmail,
          ophanId = ophanId,
          abTests = chosenVariants.asJson,
          cmp = cmp,
          intCmp = intCmp
        )
        contributionData.insertPaymentMetaData(metadata)
      }
      result recover {
        case exception: Exception => Logger.error("Unable to store contribution metadata", exception)
      }
      Right(paymentId)
    }

    execute() match {
      case Right(paymentId) => metaData(paymentId)
      case Left(error) => Left(error)
    }
  }

  def validateEvent(headers: Map[String, String], body: String): Boolean = {
    val context = apiContext.addConfiguration(Constants.PAYPAL_WEBHOOK_ID, config.paypalWebhookId)
    Event.validateReceivedEvent(context, headers.asJava, body)
  }

}
