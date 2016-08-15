package utils
import java.util.UUID

trait TransactionUtil {
  def newTransactionId: String
}

object TransactionUtilImpl extends TransactionUtil {
  override def newTransactionId: String = UUID.randomUUID.toString
}
