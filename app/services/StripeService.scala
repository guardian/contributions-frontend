package services

import cats.data.{OptionT, XorT}
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeService => MembershipStripeService}
import data.ContributionData
import models._
import cats.implicits._
import com.gu.okhttp.RequestRunners
import com.gu.stripe.Stripe.{Charge, Event}
import org.joda.time.DateTime
import play.api.libs.json.Json
import views.support.Variant

import scala.concurrent.{ExecutionContext, Future}

class StripeService(apiConfig: StripeApiConfig, metrics: ServiceMetrics, contributionData: ContributionData)(implicit ec: ExecutionContext)
  extends MembershipStripeService(apiConfig = apiConfig, RequestRunners.loggingRunner(metrics)) {

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

    println(stripeHook.amount)
    println(stripeHook.created)
    println(stripeHook.cardCountry)
    println(stripeHook.email)
    println(stripeHook.currency)

    for {
      hook <- paymentHook.toRight(s"Unable to find the stripe event identified by ${stripeHook.eventId}")
      insertResult <- contributionData.insertPaymentHook(hook)
    } yield insertResult
  }
}
