package services

import java.util.UUID

import abtests.Allocation
import cats.data.EitherT
import cats.implicits._
import com.gu.i18n
import com.gu.i18n.CountryGroup
import com.paypal.api.payments._
import com.paypal.base.Constants
import com.paypal.base.rest.APIContext
import com.typesafe.config.Config
import data.ContributionData
import models._
import monitoring.TagAwareLogger
import monitoring.LoggingTags
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode
import scala.util.Try
import scala.util.control.NonFatal

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
  emailService: EmailService,
  supportPaypalExecuteEndpoint: String
)(implicit ec: ExecutionContext) extends TagAwareLogger {
  val description = "Contribution to the guardian"
  val credentials = config.credentials

  def apiContext: APIContext = new APIContext(credentials.clientId, credentials.clientSecret, config.paypalMode)

  private def asyncExecute[A](block: => A)(implicit tags: LoggingTags): EitherT[Future, PaypalApiError, A] =
    Future(block).attemptT.leftMap { case NonFatal(throwable) => PaypalApiError.fromThrowable(throwable) }

  private def fullName(payerInfo: PayerInfo): Option[String] = {
    val firstName = Option(payerInfo.getFirstName)
    val lastName = Option(payerInfo.getLastName)
    Seq(firstName, lastName).flatten match {
      case Nil => None
      case names => Some(names.mkString(" "))
    }
  }

  def getPayment(paymentId: String)(implicit tags: LoggingTags): EitherT[Future, PaypalApiError, Payment] =
    asyncExecute(Payment.get(apiContext, paymentId))

  def getPayment(
    amount: BigDecimal,
    countryGroup: CountryGroup,
    contributionId: ContributionId,
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String],
    ophanVisitId: Option[String],
    componentId: Option[String],
    componentType: Option[ComponentType],
    source: Option[AcquisitionSource],
    refererAbTest: Option[AbTest],
    nativeAbTests: Option[Set[AbTest]],
    supportRedirect: Option[Boolean] = Some(false)
  )(implicit tags: LoggingTags): EitherT[Future, PaypalApiError, Payment] = {
    import utils.QueryStringBindableUtils.Syntax._
    import utils.ThriftUtils.Implicits._

    val paymentToCreate = {

      val returnUrl: String = {
        val extraParams = List(
          cmp.map(value => s"CMP=$value"),
          intCmp.map(value => s"INTCMP=$value"),
          ophanPageviewId.map(value => s"pvid=$value"),
          ophanBrowserId.map(value => s"bid=$value"),
          refererPageviewId.map(value => s"refererPageviewId=$value"),
          refererUrl.map(value => s"refererUrl=$value"),
          ophanVisitId.map(value => s"ophanVisitId=$value"),
          componentId.map(_.encodeQueryString("componentId")),
          componentType.map(_.encodeQueryString("componentType")),
          source.map(_.encodeQueryString("source")),
          refererAbTest.map(_.encodeQueryString("refererAbTest")),
          nativeAbTests.map(_.encodeQueryString("nativeAbTests")),
          supportRedirect.map(value => s"supportRedirect=$value")
        ).flatten match {
          case Nil => ""
          case params => params.mkString("?", "&", "")
        }

        val supportBackendExecuteEndpoint = s"$supportPaypalExecuteEndpoint$extraParams"
        val contributeBackendExecuteEndpoint = s"${config.baseReturnUrl}/paypal/${countryGroup.id}/execute$extraParams"

        supportRedirect match {
          case Some(true) => supportBackendExecuteEndpoint
          case _ => contributeBackendExecuteEndpoint
        }
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

    asyncExecute {
      paymentToCreate.create(apiContext)
    }
  }

  def executePayment(paymentId: String, payerId: String)(implicit tags: LoggingTags): EitherT[Future, PaypalApiError, Payment] = {
    val payment = new Payment().setId(paymentId)
    val paymentExecution = new PaymentExecution().setPayerId(payerId)
    asyncExecute(payment.execute(apiContext, paymentExecution)) subflatMap { payment =>
      if (payment.getState.toUpperCase != "APPROVED") {
        Left(PaypalApiError.fromString(s"payment returned with state: ${payment.getState}"))
      } else Right(payment)
    }
  }

  def attempt[A](action: String)(block: => A)(implicit tags: LoggingTags): EitherT[Future, PaypalApiError, A] = {
    Future(block).attemptT.leftMap { t: Throwable =>
      val message = s"Unable to $action"
      error(message, t)
      PaypalApiError.fromString(message)
    }
  }

  def capturePayment(paymentId: String)(implicit tags: LoggingTags): EitherT[Future, PaypalApiError, Capture] = {

    def capture(transaction: Transaction): Capture = {
      val amount = transaction.getAmount
      val amountToSend = new Amount()
      amountToSend.setCurrency(amount.getCurrency)
      amountToSend.setTotal(amount.getTotal)
      val capture = new Capture()
      capture.setAmount(amountToSend)
      capture.setIsFinalCapture(true)
      capture
    }

    val result = for {
      payment <- asyncExecute(Payment.get(apiContext, paymentId))
      transaction <- attempt("get payment transaction")(payment.getTransactions.asScala.head)
      authorisation <- attempt("get transaction auth")(transaction.getRelatedResources.asScala.head.getAuthorization)
      r <- asyncExecute(authorisation.capture(apiContext, capture(transaction)))
    } yield r

    result subflatMap { capture =>
      if (capture.getState.toUpperCase != "COMPLETED") {
        Left(PaypalApiError.fromString(s"payment returned with state: ${capture.getState}"))
      } else Right(capture)
    }
  }

  def storeMetaData(
    payment: Payment,
    testAllocations: Set[Allocation],
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String],
    idUser: Option[IdentityId],
    platform: Option[String],
    ophanVisitId: Option[String]
  )(implicit tags: LoggingTags): EitherT[Future, String, SavedContributionData] = {

    val contributionDataToSave = for {
      transaction <- attempt("get transaction")(payment.getTransactions.asScala.head)
      contributionId <- attempt("get custom field")(UUID.fromString(transaction.getCustom))
      created <- attempt("get payment date")(new DateTime(payment.getCreateTime))
      payerInfo <- attempt("get PayerInfo")(payment.getPayer.getPayerInfo)
    } yield {
      val metadata = ContributionMetaData(
        contributionId = ContributionId(contributionId),
        created = created,
        email = payerInfo.getEmail,
        country = Option(payerInfo.getCountryCode),
        ophanPageviewId = ophanPageviewId,
        ophanBrowserId = ophanBrowserId,
        abTests = testAllocations,
        cmp = cmp,
        intCmp = intCmp,
        refererPageviewId =refererPageviewId,
        refererUrl = refererUrl,
        platform = platform,
        ophanVisitId = ophanVisitId
      )

      val postCode = {
        def billingPostCode = Option(payerInfo.getBillingAddress).flatMap(address => Option(address.getPostalCode))

        def shippingPostcode = for {
          itemList <- Option(transaction.getItemList)
          shippingAddress <- Option(itemList.getShippingAddress)
        } yield {
          shippingAddress.getPostalCode
        }

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
        postCode = postCode
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
      data <- contributionDataToSave.leftMap(_.message)
      (contributor, contributionMetaData, contributorRow) = data
      contributionMetaData <- contributionData.insertPaymentMetaData(contributionMetaData)
      contributor <- contributionData.saveContributor(contributor)
      _ <- emailService.thank(contributorRow).leftMap(e => e.getMessage)
    } yield SavedContributionData(
      contributor = contributor,
      contributionMetaData = contributionMetaData
    )
  }

  def validateEvent(headers: Map[String, String], body: String): Boolean = {
    val context = apiContext.addConfiguration(Constants.PAYPAL_WEBHOOK_ID, config.paypalWebhookId)
    Event.validateReceivedEvent(context, headers.asJava, body)
  }

  def processPaymentHook(paypalHook: PaypalHook)(implicit tags: LoggingTags): EitherT[Future, String, PaymentHook] = {

    def contributionIdFromPaypal(paymentId: String): ContributionId = {
      val payment = Payment.get(apiContext, paypalHook.paymentId)
      val transaction = payment.getTransactions.asScala.head
      ContributionId(UUID.fromString(transaction.getCustom))
    }

    val contributionId = paypalHook.contributionId.getOrElse(contributionIdFromPaypal(paypalHook.paymentId))
    contributionData.insertPaymentHook(PaymentHook.fromPaypal(paypalHook, contributionId))
  }

  def paymentAmount(payment: Payment): Option[ContributionAmount] = for {
    transaction <- payment.getTransactions.asScala.headOption
    paypalAmount <- Option(transaction.getAmount)
    amount <- Try(BigDecimal.exact(paypalAmount.getTotal)).toOption
    currency <- Option(paypalAmount.getCurrency).flatMap(i18n.Currency.fromString)
  } yield ContributionAmount(amount, currency)

}
