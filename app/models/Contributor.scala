package models

import java.util.UUID

case class ContributorId(id: UUID) extends AnyVal

object ContributorId {
  def random: ContributorId = ContributorId(UUID.randomUUID())
}

case class Contributor(
  email: String,
  contributorId: Option[ContributorId],
  name: Option[String],
  firstName: Option[String],
  lastName: Option[String],
  idUser: Option[IdentityId],
  postCode: Option[String]
)
