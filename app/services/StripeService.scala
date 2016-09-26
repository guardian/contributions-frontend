package services

import java.util.UUID

import cats.data.{OptionT, XorT}
import com.gu.monitoring.StatusMetrics
import com.gu.stripe.{StripeApiConfig, StripeService => MembershipStripeService}
import data.ContributionData
import models._
import cats.implicits._
import com.gu.stripe.Stripe.{BalanceTransaction, Charge, Event}

import scala.concurrent.{ExecutionContext, Future}

class StripeService(apiConfig: StripeApiConfig, metrics: StatusMetrics, contributionData: ContributionData)(implicit ec: ExecutionContext)
  extends MembershipStripeService(apiConfig = apiConfig, metrics = metrics) {

  def storeMetaData(stripeHook: StripeHook): XorT[Future, String, SavedContributionData] = {
    val metadata = ContributionMetaData(
      contributionId = UUID.nameUUIDFromBytes(stripeHook.paymentId.getBytes),
      created = stripeHook.created,
      email = stripeHook.email,
      ophanId = Some(stripeHook.ophanId),
      abTests = stripeHook.abTests,
      cmp = stripeHook.cmp,
      intCmp = stripeHook.intCmp
    )
    val contributor = Contributor(
      email = stripeHook.email,
      name = Some(stripeHook.name),
      firstName = None,
      lastName = None,
      idUser = stripeHook.idUser,
      postCode = stripeHook.postCode,
      marketingOptIn = stripeHook.marketingOptIn
    )

    for {
      savedMetadata <- contributionData.insertPaymentMetaData(metadata)
      savedContributor <- contributionData.saveContributor(contributor)
    } yield SavedContributionData(
      contributor = contributor,
      contributionMetaData = metadata
    )
  }

  def processPaymentHook(stripeHook: StripeHook): XorT[Future, String, PaymentHook] = {

    def findCharge(stripeHook: StripeHook): XorT[Future, String, Event[Charge]] = {
      OptionT(this.Event.findCharge(stripeHook.eventId))
        .toRight(s"Impossible to find charge with eventId: ${stripeHook.eventId}")
    }

    def findBalanceTransaction(event: Event[Charge]): XorT[Future, String, BalanceTransaction] = {
      OptionT(this.BalanceTransaction.read(event.`object`.balance_transaction))
        .toRight(s"Impossible to find balance transaction with Id: ${event.`object`.balance_transaction}")
    }

    def toPaymentHook(balanceTransaction: BalanceTransaction): PaymentHook = {
      PaymentHook.fromStripe(
        stripeHook = stripeHook,
        convertedAmount = BigDecimal(balanceTransaction.amount, 2)
      )
    }

    for {
      eventFromStripe <- findCharge(stripeHook)
      balanceTransaction <- findBalanceTransaction(eventFromStripe)
      paymentHook = toPaymentHook(balanceTransaction)
      insertedPaymentHook <- contributionData.insertPaymentHook(paymentHook)
    } yield insertedPaymentHook
  }
}
