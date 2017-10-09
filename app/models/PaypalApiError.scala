package models

import com.paypal.api.payments.{Error, ErrorDetails}
import com.paypal.base.rest.PayPalRESTException
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.{Json, Writes}
import scala.collection.JavaConverters._

sealed trait PaypalErrorType extends EnumEntry

object PaypalErrorType extends Enum[PaypalErrorType] with PlayJsonEnum[PaypalErrorType] {
  val values = findValues

  case object NotFound extends PaypalErrorType
  case object PaymentAlreadyDone extends PaypalErrorType
  case object InstrumentDeclined extends PaypalErrorType
  case object Other extends PaypalErrorType

  def fromPaypalError(error: Error): PaypalErrorType = error.getName match {
    case "INVALID_RESOURCE_ID" => NotFound
    case "PAYMENT_ALREADY_DONE" => PaymentAlreadyDone
    case "INSTRUMENT_DECLINED" => InstrumentDeclined
    case _ => Other
  }
}

case class PaypalApiError(
  errorType: PaypalErrorType,
  message: String
)

object PaypalApiError {

  def fromString(message: String): PaypalApiError = PaypalApiError(PaypalErrorType.Other, message)

  def fromThrowable(exception: Throwable): PaypalApiError = exception match {

    case paypalException: PayPalRESTException =>

      val errorMessage = (for {
        error <- Option(paypalException.getDetails)
        message <- Option(error.getMessage)
        message if message != ""
      } yield message).getOrElse("Unknown error message")

      PaypalApiError(PaypalErrorType.fromPaypalError(paypalException.getDetails), errorMessage)

    case exception: Exception =>
      PaypalApiError.fromString(exception.getMessage)
  }

  implicit val paypalApiErrorWrite: Writes[PaypalApiError] = Json.writes[PaypalApiError]
}
