package models

import java.util.UUID

import models.PaymentProvider.{Paypal, Stripe}
import org.joda.time.DateTime
import play.api.libs.json._

sealed trait PaymentProvider

object PaymentProvider extends EnumMapping[PaymentProvider] {
  case object Paypal extends PaymentProvider
  case object Stripe extends PaymentProvider

  val mapping: Map[PaymentProvider, String] = Map(
    Paypal -> "Paypal",
    Stripe -> "Stripe"
  )
}

sealed trait PaymentStatus

object PaymentStatus extends EnumMapping[PaymentStatus] {
  case object Failed extends PaymentStatus
  case object Paid extends PaymentStatus
  case object Refunded extends PaymentStatus

  val mapping: Map[PaymentStatus, String] = Map(
    Failed -> "Failed",
    Paid -> "Paid",
    Refunded -> "Refunded"
  )

  val paypalReads = new Reads[PaymentStatus] {
    override def reads(json: JsValue): JsResult[PaymentStatus] = json match {
      case JsString("PAYMENT.SALE.COMPLETED") => JsSuccess(Paid)
      case JsString("PAYMENT.SALE.DENIED") => JsSuccess(Failed)
      case JsString("PAYMENT.SALE.REFUNDED") => JsSuccess(Refunded)
      case JsString(wrongStatus) => JsError(s"Unexpected paypal status: $wrongStatus")
      case _ => JsError("Unknown paypal status type, a JsString was expected")
    }
  }

  val stripeReads = new Reads[PaymentStatus] {
    override def reads(json: JsValue): JsResult[PaymentStatus] = json match {
      case JsString("succeeded") => JsSuccess(Paid)
      case JsString("failed") => JsSuccess(Failed)
      case JsString("refunded") => JsSuccess(Refunded)
      case JsString(wrongStatus) => JsError(s"Unexpected stripe status: $wrongStatus")
      case _ => JsError("Unknown stripe status type, a JsString was expected")
    }
  }
}

case class PaymentHook(
  contributionId: UUID,
  paymentId: String,
  provider: PaymentProvider,
  created: DateTime,
  currency: String,
  cardCountry: Option[String],
  amount: BigDecimal,
  convertedAmount: Option[BigDecimal],
  status: PaymentStatus,
  email: Option[String]
)

object PaymentHook {
  def fromPaypal(paypalHook: PaypalHook): PaymentHook = PaymentHook(
    contributionId = paypalHook.contributionId,
    paymentId = paypalHook.paymentId,
    provider = Paypal,
    created = paypalHook.created,
    currency = paypalHook.currency,
    cardCountry = None,
    amount = paypalHook.amount,
    convertedAmount = None,
    status = paypalHook.status,
    email = None
  )

  def fromStripe(stripeHook: StripeHook, contributionId: UUID, convertedAmount: BigDecimal): PaymentHook = PaymentHook(
    contributionId = contributionId,
    paymentId = stripeHook.paymentId,
    provider = Stripe,
    created = stripeHook.created,
    currency = stripeHook.currency,
    cardCountry = Some(stripeHook.cardCountry),
    amount = stripeHook.amount,
    convertedAmount = Some(convertedAmount),
    status = stripeHook.status,
    email = Some(stripeHook.email)
  )
}

case class PaypalHook(
  contributionId: UUID,
  paymentId: String,
  created: DateTime,
  currency: String,
  amount: BigDecimal,
  status: PaymentStatus
)

object PaypalHook {
  implicit val reader = new Reads[PaypalHook] {
    override def reads(json: JsValue): JsResult[PaypalHook] = {
      for {
        resource <- (json \ "resource").validate[JsObject]
        contributionId <- (resource \ "custom").validate[UUID]
        paymentId <- (resource \ "parent_payment").validate[String]
        created <- (resource \ "create_time").validate[String]
        currency <- (resource \ "amount" \ "currency").validate[String]
        amount <- (resource \ "amount" \ "total").validate[BigDecimal]
        status <- (json \ "event_type").validate[PaymentStatus](PaymentStatus.paypalReads)
      } yield PaypalHook(
        contributionId = contributionId,
        paymentId = paymentId,
        created = new DateTime(created),
        currency = currency,
        amount = amount,
        status = status
      )
    }
  }
}

case class StripeHook(
  eventId: String,
  paymentId: String,
  balanceTransactionId: String,
  created: DateTime,
  currency: String,
  amount: BigDecimal,
  cardCountry: String,
  status: PaymentStatus,
  name: String,
  email: String,
  postCode: Option[String],
  ophanId: String,
  abTests: JsValue,
  cmp: Option[String],
  intCmp: Option[String],
  marketingOptIn: Option[Boolean]
)

object StripeHook {
  implicit val reader = new Reads[StripeHook] {
    override def reads(json: JsValue): JsResult[StripeHook] = {
      for {
        eventId <- (json \ "id").validate[String]
        payload <- (json \ "data" \ "object").validate[JsObject]
        metadata <- (payload \ "metadata").validate[JsObject]
        paymentId <- (payload \ "id").validate[String]
        created <- (payload \ "created").validate[Long]
        currency <- (payload \ "currency").validate[String]
        amount <- (payload \ "amount").validate[Long]
        cardCountry <- (payload \ "source" \ "country").validate[String]
        status <- (payload \ "status").validate[PaymentStatus](PaymentStatus.stripeReads)
        name <- (metadata \ "name").validate[String]
        email <- (metadata \ "email").validate[String]
        postCode <- (metadata \ "postcode").validateOpt[String]
        ophanId <- (metadata \ "ophanId").validate[String]
        abTests <- (metadata \ "abTests").validate[String].map(Json.parse)
        cmp <- (metadata \ "cmp").validateOpt[String]
        intCmp <- (metadata \ "intcmp").validateOpt[String]
        marketingOptIn <- (metadata \ "marketing-opt-in").validateOpt[String]
        balanceTransactionId <- (payload \ "balance_transaction").validate[String]
      } yield {
        StripeHook(
          eventId = eventId,
          paymentId = paymentId,
          balanceTransactionId = balanceTransactionId,
          created = new DateTime(created),
          currency = currency.toUpperCase,
          amount = BigDecimal(amount, 2),
          cardCountry = cardCountry,
          status = status,
          name = name,
          email = email,
          postCode = postCode,
          ophanId = ophanId,
          abTests = abTests,
          cmp = cmp,
          intCmp = intCmp,
          marketingOptIn = marketingOptIn.map(_.toBoolean)
        )
      }
    }
  }
}
