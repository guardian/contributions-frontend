package utils

/**
 * Useful when handling errors with Xor.
 * Allows for any recovery logic at the end of the 'flow' to be dependent on the specific error type.
 */
sealed trait AppError {
  def message: String
}

case class PaypalPaymentError(message: String) extends AppError

case class StoreMetaDataError(message: String) extends AppError
