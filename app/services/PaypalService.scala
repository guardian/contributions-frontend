package services

import java.util.UUID

import cats.data.{Xor, XorT}
import cats.implicits._
import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.paypal.api.payments._
import com.paypal.base.Constants
import com.paypal.base.rest.APIContext

import scala.collection.JavaConverters._
import com.typesafe.config.Config
import data.ContributionData
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import views.support.Variant

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode
import scala.util.Try

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

class PaypalService(config: PaypalApiConfig, contributionData: ContributionData)(implicit ec: ExecutionContext) {
  val description = "Contribution to the guardian"
  val credentials = config.credentials

  def apiContext: APIContext = new APIContext(credentials.clientId, credentials.clientSecret, config.paypalMode)

  private def asyncExecute[A](block: => A): XorT[Future, String, A] = XorT(Future {
    val result = Try(block)
    Xor.fromTry(result).leftMap { exception =>
      Logger.error("Error while calling PayPal API", exception)
      exception.getMessage
    }
  })

  def getAuthUrl(
    amount: BigDecimal,
    countryGroup: CountryGroup,
    contributionId: String,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  ): XorT[Future, String, Uri] = {

    val paymentToCreate = {

      val returnUrl: String = {
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
      new Payment().setIntent("sale").setPayer(payer).setTransactions(transactions).setRedirectUrls(redirectUrls)
    }

    asyncExecute{
      paymentToCreate.create(apiContext)
    } transform {
      case Xor.Right(createdPayment) => Xor.fromOption(getAuthLink(createdPayment), "No approval link returned from PayPal")
      case Xor.Left(error) => Xor.left(error)
    }
  }

  private def getAuthLink(payment: Payment) = {
    for {
      links <- Option(payment.getLinks)
      approvalLinks <- links.asScala.find(_.getRel.equalsIgnoreCase("approval_url"))
      authUrl <- Option(approvalLinks.getHref)
    }
      yield {
        Uri.parse(authUrl).addParam("useraction", "commit")
      }
  }

  def executePayment(paymentId: String, payerId: String): XorT[Future, String, Payment] = {
    val payment = new Payment().setId(paymentId)
    val paymentExecution = new PaymentExecution().setPayerId(payerId)
    asyncExecute(payment.execute(apiContext, paymentExecution)) transform  {
      case Xor.Right(payment) if (payment.getState.toUpperCase != "APPROVED") => Xor.left(s"payment returned with state: ${payment.getState}")
      case Xor.Right(payment) => Xor.right(payment)
      case Xor.Left(error) => Xor.left(error)
    }

  }

  case class SavedContributionData(contributor: Contributor, contributionMetaData: ContributionMetaData)

  def storeMetaData(
    paymentId: String,
    variants: Seq[Variant],
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String],
    idUser: Option[String]
  ): XorT[Future, String, SavedContributionData] = {
    val triedSavedContributionData = for {
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
        abTests = Json.toJson(variants),
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

      SavedContributionData(
        contributor = contributor,
        contributionMetaData = metadata
      )
    }

    val contributionDataToSave = Xor.fromTry(triedSavedContributionData).leftMap { exception =>
      Logger.error("Unable to store contribution metadata", exception)
      "Unable to store contribution metadata"
    }

    for {
      data <- XorT.fromXor[Future](contributionDataToSave)
      contributionMetaData <- contributionData.insertPaymentMetaData(data.contributionMetaData)
      contributor <- contributionData.saveContributor(data.contributor)
    } yield SavedContributionData(
      contributor = contributor,
      contributionMetaData = contributionMetaData
    )
  }

  def updateMarketingOptIn(email: String, marketingOptInt: Boolean): XorT[Future, String, Contributor] = {
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

  def processPaymentHook(paymentHook: PaymentHook): XorT[Future, String, PaymentHook] = {
    contributionData.insertPaymentHook(paymentHook)
  }

}
