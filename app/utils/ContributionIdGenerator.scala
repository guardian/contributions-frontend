package utils
import java.util.UUID

trait ContributionIdGenerator {
  def getNewId: String
}

object ContributionIdGeneratorImpl extends ContributionIdGenerator {
  override def getNewId: String = UUID.randomUUID.toString
}
