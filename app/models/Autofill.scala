package models

import play.api.libs.json.{Format, Json}

case class Autofill(name: Option[String], email: Option[String])
object Autofill {
  val empty = Autofill(None, None)
  implicit val jf: Format[Autofill] = Json.format[Autofill]
}
