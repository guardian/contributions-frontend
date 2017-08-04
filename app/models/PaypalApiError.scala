package models

import com.paypal.api.payments.Error
import enumeratum.{Enum, EnumEntry}


sealed trait PaypalErrorType extends EnumEntry

object PaypalErrorType extends Enum[PaypalErrorType] {
  val values = findValues

  case object NotFound extends PaypalErrorType
  case object Other extends PaypalErrorType

  def fromPaypalError(error: Error): PaypalErrorType = error.getName match {
    case "INVALID_RESOURCE_ID" => NotFound
    case _ => Other
  }
}

case class PaypalApiError(
  errorType: PaypalErrorType,
  message: String
)

object PaypalApiError {
  def apply(message: String): PaypalApiError = PaypalApiError(PaypalErrorType.Other, message)
}
