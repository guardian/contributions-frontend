package services

import abtests.Allocation
import cats.data.{EitherT, OptionT}
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeService => MembershipStripeService}
import data.ContributionData
import models._
import cats.implicits._
import com.gu.okhttp.RequestRunners
import com.gu.stripe.Stripe.{Charge, Event}
import monitoring.LoggingTags
import monitoring.TagAwareLogger
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class StripeService(
  apiConfig: StripeApiConfig,
  metrics: ServiceMetrics,
  contributionData: ContributionData,
  identityService: IdentityService,
  emailService: EmailService
)(implicit ec: ExecutionContext)
  extends MembershipStripeService(apiConfig = apiConfig, RequestRunners.loggingRunner(metrics)) {

  case class StripeMetaData(contributionMetadata: ContributionMetaData, contributor: Contributor, contributorRow: ContributorRow)

  def createMetaData(
    contributionId: ContributionId,
    charge: Charge,
    created: DateTime,
    name: String,
    postCode: Option[String],
    testAllocations: Set[Allocation],
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: String,
    ophanBrowserId: Option[String],
    idUser: Option[IdentityId],
    platform: Option[String],
    ophanVisitId: Option[String]
  )(implicit tags: LoggingTags): StripeMetaData = {

    val metadata = ContributionMetaData(
      contributionId = contributionId,
      created = created,
      email = charge.receipt_email,
      country = Some(charge.source.country),
      ophanPageviewId = Some(ophanPageviewId),
      ophanBrowserId = ophanBrowserId,
      abTests = testAllocations,
      cmp = cmp,
      intCmp = intCmp,
      refererPageviewId = refererPageviewId,
      refererUrl = refererUrl,
      platform = platform,
      ophanVisitId = ophanVisitId
    )

    val contributor = Contributor(
      email = charge.receipt_email,
      contributorId = Some(ContributorId.random),
      name = Some(name),
      firstName = None,
      lastName = None,
      idUser = idUser,
      postCode = postCode
    )

    val contributorRow = ContributorRow(
      email = charge.receipt_email,
      created = created,
      amount = BigDecimal(charge.amount, 2),
      currency = charge.currency.toUpperCase,
      name = name,
      cmp = cmp
    )

    StripeMetaData(metadata, contributor, contributorRow)
  }

  def storeMetaData(
    created: DateTime,
    name: String,
    cmp: Option[String],
    metadata: ContributionMetaData,
    contributor: Contributor,
    contributorRow: ContributorRow,
    idUser: Option[IdentityId])
    (implicit tags: LoggingTags): EitherT[Future, String, SavedContributionData] = {
      emailService.thank(contributorRow)

      for {
        savedMetadata <- contributionData.insertPaymentMetaData(metadata)
        savedContributor <- contributionData.saveContributor(contributor)
      } yield SavedContributionData(
        contributor = contributor,
        contributionMetaData = metadata
      )
    }

  def processPaymentHook(stripeHook: StripeHook)(implicit tags: LoggingTags): EitherT[Future, String, PaymentHook] = {

    def convertedAmount(event: Event[Charge]): OptionT[Future, BigDecimal] = {
      for {
        balanceTransactionId <- OptionT.fromOption[Future](event.`object`.balance_transaction)
        balanceTransaction <- OptionT(this.BalanceTransaction.read(balanceTransactionId))
      } yield BigDecimal(balanceTransaction.amount, 2)
    }

    def toPaymentHook(event: Event[Charge]): OptionT[Future, PaymentHook] = OptionT.liftF {
      convertedAmount(event).value.map { amount =>
        PaymentHook.fromStripe(stripeHook = stripeHook, convertedAmount = amount)
      }
    }

    val paymentHook = for {
      event <- OptionT(this.Event.findCharge(stripeHook.eventId))
      paymentHook <- toPaymentHook(event)
    } yield paymentHook

    for {
      hook <- paymentHook.toRight(s"Unable to find the stripe event identified by ${stripeHook.eventId}")
      insertResult <- contributionData.insertPaymentHook(hook)
    } yield insertResult
  }
}
