package services

import cats.data.{OptionT, XorT}
import com.gu.monitoring.StatusMetrics
import com.gu.stripe.{StripeApiConfig, StripeService => MembershipStripeService}
import data.ContributionData
import models._
import cats.implicits._
import com.gu.stripe.Stripe.{BalanceTransaction, Charge, Event}
import org.joda.time.DateTime
import play.api.libs.json.Json
import views.support.Variant

import scala.concurrent.{ExecutionContext, Future}

class StripeService(apiConfig: StripeApiConfig, metrics: StatusMetrics, contributionData: ContributionData)(implicit ec: ExecutionContext)
  extends MembershipStripeService(apiConfig = apiConfig, metrics = metrics) {

  def storeMetaData(
    contributionId: ContributionId,
    created: DateTime,
    email: String,
    name: String,
    postCode: Option[String],
    marketing: Boolean,
    variants: Seq[Variant],
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: String,
    idUser: Option[String]
  ): XorT[Future, String, SavedContributionData] = {
    val metadata = ContributionMetaData(
      contributionId = contributionId,
      created = created,
      email = email,
      ophanId = Some(ophanId),
      abTests = Json.toJson(variants),
      cmp = cmp,
      intCmp = intCmp
    )
    val contributor = Contributor(
      email = email,
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

    println(stripeHook.amount)
    println(stripeHook.created)
    println(stripeHook.cardCountry)
    println(stripeHook.email)
    println(stripeHook.currency)

    for {
      eventFromStripe <- findCharge(stripeHook)
      balanceTransaction <- findBalanceTransaction(eventFromStripe)
      paymentHook = toPaymentHook(balanceTransaction)
      insertedPaymentHook <- contributionData.insertPaymentHook(paymentHook)
    } yield insertedPaymentHook
  }
}
