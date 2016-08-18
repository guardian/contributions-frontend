package models

import play.api.libs.json._

trait EnumMapping[A] {
  val mapping: Map[A, String]
  lazy val reverseMapping: Map[String, A] = mapping.map { case (key, value) => value -> key}

  def enumToString(a: A): String = mapping(a)
  def enumFromString(str: String): Option[A] = reverseMapping.get(str)
  def enumFromString(str: String, default: A): A = reverseMapping.getOrElse(str, default)

  implicit val jf = new Format[A] {
    override def writes(o: A): JsValue = JsString(enumToString(o))
    override def reads(json: JsValue): JsResult[A] = json match {
      case JsString(value) => enumFromString(value).map(JsSuccess(_)).getOrElse(JsError(s"Unkown enum value: $value"))
      case _ => JsError("Wrong enum type, a JsString is expected")
    }
  }
}
