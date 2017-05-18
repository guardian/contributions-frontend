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
import monitoring.SentryLoggingTags
import monitoring.SentryTagLogger
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

  def storeMetaData(
    contributionId: ContributionId,
    charge: Charge,
    created: DateTime,
    name: String,
    postCode: Option[String],
    marketing: Boolean,
    testAllocations: Set[Allocation],
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: String,
    ophanBrowserId: Option[String],
    idUser: Option[IdentityId]
  )(implicit tags: SentryLoggingTags): EitherT[Future, String, SavedContributionData] = {

    // Fire and forget: we don't want to stop the user flow
    idUser.map { id =>
      identityService.updateMarketingPreferences(id, marketing)
    }
    emailService.thank(ContributorRow(
      email = charge.receipt_email,
      created = created,
      amount = BigDecimal(charge.amount, 2),
      currency = charge.currency.toUpperCase,
      name = name,
      cmp = cmp
    ))

    val metadata = ContributionMetaData(
      contributionId = contributionId,
      created = created,
      email = charge.receipt_email,
      country = Some(charge.source.country),
      ophanPageviewId = Some(ophanPageviewId),
      ophanBrowserId = ophanBrowserId,
      abTests = Json.toJson(testAllocations),
      cmp = cmp,
      intCmp = intCmp,
      refererPageviewId = refererPageviewId,
      refererUrl = refererUrl
    )
    val contributor = Contributor(
      email = charge.receipt_email,
      contributorId = Some(ContributorId.random),
      name = Some(name),
      firstName = None,
      lastName = None,
      idUser = idUser,
      postCode = postCode,
      marketingOptIn = Some(marketing)
    )

    for {
      savedMetadata <- contributionData.insertPaymentMetaData(metadata)
      savedContributor <- contributionData.saveContributor(contributor)
    } yield SavedContributionData(
      contributor = contributor,
      contributionMetaData = metadata
    )
  }

  def processPaymentHook(stripeHook: StripeHook)(implicit tags: SentryLoggingTags): EitherT[Future, String, PaymentHook] = {

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
