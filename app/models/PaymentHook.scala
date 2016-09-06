package models

import java.util.UUID

import models.PaymentProvider.Paypal
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

  implicit val paypalReader = new Reads[PaymentHook] {

    override def reads(json: JsValue): JsResult[PaymentHook] = {
      for {
        resource <- (json \ "resource").validate[JsObject]
        contributionId <- (resource \ "custom").validate[UUID]
        paymentId <- (resource \ "parent_payment").validate[String]
        created <- (resource \ "create_time").validate[String]
        currency <- (resource \ "amount" \ "currency").validate[String]
        amount <- (resource \ "amount" \ "total").validate[BigDecimal]
        status <- (json \ "event_type").validate[PaymentStatus](PaymentStatus.paypalReads)
      } yield PaymentHook(
        contributionId = contributionId,
        paymentId = paymentId,
        provider = Paypal,
        created = new DateTime(created),
        currency = currency,
        cardCountry = None,
        amount = amount,
        convertedAmount = None,
        status = status,
        email = None
      )
    }
  }
}
