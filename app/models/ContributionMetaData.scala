package models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class ContributionMetaData(
  contributionId: UUID,
  created: DateTime,
  email: String,
  ophanId: Option[String],
  abTests: JsValue,
  cmp: Option[String],
  intCmp: Option[String]
)
