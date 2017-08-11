package controllers.httpmodels

import models.IdentityId
import play.api.libs.json.{Json, Reads}

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
  implicit val captureRequestReads: Reads[CaptureRequest] = Json.reads[CaptureRequest]
}
