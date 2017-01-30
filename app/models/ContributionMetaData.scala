package models

import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class ContributionMetaData(
  contributionId: ContributionId,
  created: DateTime,
  email: String,
  country: Option[String],
  ophanPageviewId: Option[String],
  ophanBrowserId: Option[String],
  abTests: JsValue,
  cmp: Option[String],
  intCmp: Option[String],
  refererPageviewId: Option[String],
  refererUrl: Option[String]
)
