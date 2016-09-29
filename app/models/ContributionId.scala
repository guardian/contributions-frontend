package models

import java.util.UUID

import play.api.libs.json._

case class ContributionId(id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

object ContributionId {
  def random: ContributionId = ContributionId(UUID.randomUUID())

  def apply(id: String): ContributionId = ContributionId(UUID.fromString(id))

  implicit val jf = new Format[ContributionId] {
    override def reads(json: JsValue): JsResult[ContributionId] = json.validate[UUID].map(ContributionId.apply)
    override def writes(o: ContributionId): JsValue = Json.toJson(o.id)
  }
}
