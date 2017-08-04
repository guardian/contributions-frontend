package models

import com.paypal.api.payments.Error
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.{JsValue, Json, Writes}


sealed trait PaypalErrorType extends EnumEntry

object PaypalErrorType extends Enum[PaypalErrorType] with PlayJsonEnum[PaypalErrorType] {
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

  // defining the Writes manually, otherwise Play-Json gets offended by the overloaded "apply" just above
  implicit val jf: Writes[PaypalApiError] = new Writes[PaypalApiError] {
    override def writes(o: PaypalApiError): JsValue = Json.obj(
      "errorType" -> o.errorType,
      "message" -> o.message
    )
  }
}
