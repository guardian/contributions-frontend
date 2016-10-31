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
  def from(config: Config, environmentName: String) = PaypalApiConfig(
    envName = environmentName,
    credentials = PaypalCredentials(config.getString("clientId"), config.getString("clientSecret")),
    paypalMode = config.getString("paypalMode"),
    baseReturnUrl = config.getString("baseReturnUrl"),
    paypalWebhookId = config.getString("paypalWebhookId")
  )
}

class PaypalService(
  config: PaypalApiConfig,
  contributionData: ContributionData,
  identityService: IdentityService,
  emailService: EmailService
)(implicit ec: ExecutionContext) {
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

  private def fullName(payerInfo: PayerInfo): Option[String] = {
    val firstName = Option(payerInfo.getFirstName)
    val lastName = Option(payerInfo.getLastName)
    Seq(firstName, lastName).flatten match {
      case Nil => None
      case names => Some(names.mkString(" "))
    }
  }

  def getAuthUrl(
    amount: BigDecimal,
    countryGroup: CountryGroup,
    contributionId: ContributionId,
    cmp: Option[String],
    intCmp: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String]
  ): XorT[Future, String, Uri] = {

    val paymentToCreate = {

      val returnUrl: String = {
        val extraParams = List(
          cmp.map(value => s"CMP=$value"),
          intCmp.map(value => s"INTCMP=$value"),
          ophanPageviewId.map(value => s"pvid=$value"),
          ophanBrowserId.map(value => s"bid=$value")
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
      transaction.setCustom(contributionId.id.toString)
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
      case Xor.Right(p) if p.getState.toUpperCase != "APPROVED" => Xor.left(s"payment returned with state: ${payment.getState}")
      case Xor.Right(p) => Xor.right(p)
      case Xor.Left(error) => Xor.left(error)
    }

  }

  def storeMetaData(
    paymentId: String,
    variants: Seq[Variant],
    cmp: Option[String],
    intCmp: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String],
    idUser: Option[IdentityId]
  ): XorT[Future, String, SavedContributionData] = {

    def tryToXorT[A](block: => A): XorT[Future, String, A] = {
      XorT.fromXor[Future](Xor.catchNonFatal(block).leftMap { t: Throwable =>
        Logger.error("Unable to store contribution metadata", t)
        "Unable to store contribution metadata"
      })
    }

    val contributionDataToSave = for {
      payment <- asyncExecute(Payment.get(apiContext, paymentId))
      transaction <- tryToXorT(payment.getTransactions.asScala.head)
      contributionId <- tryToXorT(UUID.fromString(transaction.getCustom))
      created <- tryToXorT(new DateTime(payment.getCreateTime))
      payerInfo <- tryToXorT(payment.getPayer.getPayerInfo)
    } yield {
      val metadata = ContributionMetaData(
        contributionId = ContributionId(contributionId),
        created = created,
        email = payerInfo.getEmail,
        ophanPageviewId = ophanPageviewId,
        ophanBrowserId = ophanBrowserId,
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

      val contributor = Contributor(
        email = payerInfo.getEmail,
        contributorId = Some(ContributorId.random),
        name = fullName(payerInfo),
        firstName = firstName,
        lastName = lastName,
        idUser = idUser,
        postCode = postCode,
        marketingOptIn = None
      )

      val contributorRow = ContributorRow(
        email = payerInfo.getEmail,
        created = created,
        amount = BigDecimal(transaction.getAmount.getTotal),
        currency = transaction.getAmount.getCurrency,
        name = fullName(payerInfo).getOrElse(""),
        cmp = cmp
      )

      (contributor, metadata, contributorRow)
    }

    for {
      data <- contributionDataToSave
      (contributor, contributionMetaData, contributorRow) = data
      contributionMetaData <- contributionData.insertPaymentMetaData(contributionMetaData)
      contributor <- contributionData.saveContributor(contributor)
      _ <- emailService.thank(contributorRow).leftMap(e => e.getMessage)
    } yield SavedContributionData(
      contributor = contributor,
      contributionMetaData = contributionMetaData
    )
  }

  def updateMarketingOptIn(email: String, marketingOptInt: Boolean, idUser: Option[IdentityId]): XorT[Future, String, Contributor] = {
    val contributor = Contributor(
      email = email,
      contributorId = None,
      marketingOptIn = Some(marketingOptInt),
      name = None,
      firstName = None,
      lastName = None,
      idUser = None,
      postCode = None
    )

    // Fire and forget: we don't want to stop the user flow
    idUser.map { id =>
      identityService.updateMarketingPreferences(id, marketingOptInt)
    }

    contributionData.saveContributor(contributor)
  }

  def validateEvent(headers: Map[String, String], body: String): Boolean = {
    val context = apiContext.addConfiguration(Constants.PAYPAL_WEBHOOK_ID, config.paypalWebhookId)
    Event.validateReceivedEvent(context, headers.asJava, body)
  }

  def processPaymentHook(paypalHook: PaypalHook): XorT[Future, String, PaymentHook] = {

    def countryCode(rawPayment: Payment): Option[String] = for {
      payment <- Option(rawPayment)
      payer <- Option(payment.getPayer)
      info <- Option(payer.getPayerInfo)
      countryCode <- Option(info.getCountryCode)
    } yield countryCode

    for {
      payment <- asyncExecute(Payment.get(apiContext, paypalHook.paymentId))
      result <- contributionData.insertPaymentHook(PaymentHook.fromPaypal(paypalHook, countryCode(payment)))
    } yield result
  }



}
