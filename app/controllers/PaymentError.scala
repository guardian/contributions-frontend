package controllers

sealed trait PaymentError {
  def message: String
}

object PaymentError {
  val allErrors: Map[String, PaymentError] = Map(PaypalError.toString -> PaypalError)

  def fromString(errorCode: String): Option[PaymentError] = allErrors.get(errorCode)
}

case object PaypalError extends PaymentError {
  override def message = "Your contribution using PayPal could not be processed. Please try again or contribute by credit/debit card."
}
