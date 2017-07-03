package models

import abtests.Allocation
import org.joda.time.DateTime
import play.api.libs.json.Json

case class ContributionMetaData(
  contributionId: ContributionId,
  created: DateTime,
  email: String,
  country: Option[String],
  ophanPageviewId: Option[String],
  ophanBrowserId: Option[String],
  abTests: Set[Allocation],
  cmp: Option[String],
  intCmp: Option[String],
  refererPageviewId: Option[String],
  refererUrl: Option[String],
  platform: Option[String],
  ophanVisitId: Option[String]
) {
  val abTestAsJson = Json.toJson(abTests)
}

