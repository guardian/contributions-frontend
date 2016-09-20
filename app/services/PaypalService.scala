package services

import java.util.UUID

import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.paypal.api.payments._
import com.paypal.base.Constants
import com.paypal.base.rest.{APIContext, PayPalRESTException}

import scala.collection.JavaConverters._
import com.typesafe.config.Config
import data.ContributionData
import models.{ContributionMetaData, Contributor, PaymentHook}
import org.joda.time.DateTime
import play.api.Logger
import views.support.ChosenVariants

import scala.math.BigDecimal.RoundingMode
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
    val stringAmount = amount.setScale(2, RoundingMode.HALF_UP).toString
    val paypalAmount = new Amount().setCurrency(currencyCode).setTotal(stringAmount)
    val item = new Item().setDescription(description).setCurrency(currencyCode).setPrice(stringAmount).setQuantity("1")
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

    Try {
      val createdPayment = payment.create(apiContext)
      createdPayment.getLinks.asScala
    } match {
      case Success(links) =>
        val approvalLink = links.find(_.getRel.equalsIgnoreCase("approval_url")).map(l => Right(addUserActionParam(l.getHref)))
        approvalLink.getOrElse(Left("No approval link returned from paypal"))
      case Failure(exception) => Left(exception.getMessage)
    }
  }

  private def addUserActionParam(url:String) = Uri.parse(url).addParam("useraction", "commit").toString

  def executePayment(paymentId: String, payerId: String): Either[String, Payment] = {
    val payment = new Payment().setId(paymentId)
    val paymentExecution = new PaymentExecution().setPayerId(payerId)

    Try(payment.execute(apiContext, paymentExecution)) match {
      case Success(payment) =>
        if (payment.getState.toUpperCase != "APPROVED") {
          Left(s"payment returned with state: ${payment.getState}")
        } else {
          Right(payment)
        }
      case Failure(exception) =>
        Logger.error("Unable to execute payment", exception)
        Left(exception.getMessage)
    }
  }

  case class SavedContribution(contributionMetaData: ContributionMetaData, contributor: Contributor)

  def storeMetaData(
    paymentId: String,
    chosenVariants: ChosenVariants,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String],
    idUser: Option[String]
  ): Either[String,SavedContribution ] = {
    val result = for {
      payment <- Try(Payment.get(apiContext, paymentId))
      transaction <- Try(payment.getTransactions.asScala.head)
      contributionId <- Try(UUID.fromString(transaction.getCustom))
      created <- Try(new DateTime(payment.getCreateTime))
      payerInfo <- Try(payment.getPayer.getPayerInfo)
    } yield {
      val metadata = ContributionMetaData(
        contributionId = contributionId,
        created = created,
        email = payerInfo.getEmail,
        ophanId = ophanId,
        abTests = chosenVariants.asJson,
        cmp = cmp,
        intCmp = intCmp
      )

      val postCode = {
        def billingPostCode = Option(payerInfo.getBillingAddress).flatMap(address => Option(address.getPostalCode))
        def shippingPostcode = Option(payerInfo.getShippingAddress).flatMap(address => Option(address.getPostalCode))

        billingPostCode orElse shippingPostcode
      }

      val firstName = Option(payerInfo.getFirstName)
      val lastName = Option(payerInfo.getLastName)
      val fullName = Seq(firstName, lastName).flatten match {
        case Nil => None
        case names => Some(names.mkString(" "))
      }

      val contributor = Contributor(
        email = payerInfo.getEmail,
        name = fullName,
        firstName = firstName,
        lastName = lastName,
        idUser = idUser,
        postCode = postCode,
        marketingOptIn = None
      )

      contributionData.insertPaymentMetaData(metadata)
      contributionData.saveContributor(contributor)
      SavedContribution(metadata,contributor)
    }

    result match {
      case Success(data) => Right(data)
      case Failure(exception) =>
        Logger.error("Unable to store contribution metadata", exception)
        Left("Unable to store contribution metadata")
    }

  }

  def updateMarketingOptIn(email: String, marketingOptInt: Boolean) = {
    val contributor = Contributor(
      email = email,
      marketingOptIn = Some(marketingOptInt),
      name = None,
      firstName = None,
      lastName = None,
      idUser = None,
      postCode = None
    )
    contributionData.saveContributor(contributor)
  }

  def validateEvent(headers: Map[String, String], body: String): Boolean = {
    val context = apiContext.addConfiguration(Constants.PAYPAL_WEBHOOK_ID, config.paypalWebhookId)
    Event.validateReceivedEvent(context, headers.asJava, body)
  }

  def processPaymentHook(paymentHook: PaymentHook) = contributionData.insertPaymentHook(paymentHook)

}
