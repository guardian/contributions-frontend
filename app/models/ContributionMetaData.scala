package models

import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class ContributionMetaData(
  contributionId: ContributionId,
  created: DateTime,
  email: String,
  ophanId: Option[String],
  abTests: JsValue,
  cmp: Option[String],
  intCmp: Option[String]
)
