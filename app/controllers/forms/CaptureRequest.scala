package controllers.forms

import models.IdentityId
import play.api.libs.json.{Format, Json}

case class CaptureRequest (
  paymentId: String,
  platform: String,
  idUser: Option[IdentityId],
  cmp: Option[String],
  intCmp: Option[String],
  refererPageviewId: Option[String],
  refererUrl: Option[String],
  ophanPageviewId: Option[String],
  ophanBrowserId: Option[String])

object CaptureRequest {
  implicit val jf: Format[CaptureRequest] = Json.format[CaptureRequest]
}
