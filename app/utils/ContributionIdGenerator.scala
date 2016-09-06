package utils
import java.util.UUID

object ContributionIdGenerator {
  def getNewId: String = UUID.randomUUID.toString
}
