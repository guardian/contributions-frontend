package models

case class Contributor(
  email: String,
  name: Option[String],
  firstName: Option[String],
  lastName: Option[String],
  idUser: Option[IdentityId],
  postCode: Option[String],
  marketingOptIn: Option[Boolean]
)
