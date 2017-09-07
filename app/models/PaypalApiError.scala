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
  case object Other extends PaypalErrorType

  def fromPaypalError(error: Error): PaypalErrorType = error.getName match {
    case "INVALID_RESOURCE_ID" => NotFound
    case "PAYMENT_ALREADY_DONE" => PaymentAlreadyDone
    case _ => Other
  }
}

case class PaypalApiError(errorType: PaypalErrorType, message: String) extends Exception {
  override def getMessage: String = s"PaypalApiError of type $errorType: $message"
}

object PaypalApiError {
  def fromString(message: String): PaypalApiError = PaypalApiError(PaypalErrorType.Other, message)

  private def detailToString(details: ErrorDetails): String =
    s"""
       |field: ${details.getField}
       |issue: ${details.getIssue}
       |""".stripMargin

  def fromThrowable(exception: Throwable): PaypalApiError = exception match {
    case paypalException: PayPalRESTException =>
      val details = Option(paypalException.getDetails)
        .flatMap(d => Option(d.getDetails))
        .map(_.asScala).getOrElse(Nil)
        .map(detailToString)
        .mkString(";\n")
      PaypalApiError(PaypalErrorType.fromPaypalError(paypalException.getDetails), details)
    case exception: Exception =>
      PaypalApiError.fromString(exception.getMessage)
  }

  implicit val paypalApiErrorWrite: Writes[PaypalApiError] = Json.writes[PaypalApiError]
}
