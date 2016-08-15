package utils
import java.util.UUID

trait TransactionUtils {
  def newTransactionId: String
}

object SimpleTransactionUtils extends TransactionUtils {
  override def newTransactionId: String = UUID.randomUUID.toString
}
