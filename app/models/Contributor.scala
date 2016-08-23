package models

case class Contributor(
  email: String,
  name: Option[String],
  firstName: String,
  lastName: String,
  idUser: Option[String],
  postCode: Option[String],
  marketingOptIn: Option[Boolean]
)
