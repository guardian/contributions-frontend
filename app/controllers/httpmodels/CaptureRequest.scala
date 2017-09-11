package controllers.httpmodels

import models.IdentityId
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.libs.json._

case class CaptureRequest (
  paymentId: String,
  platform: String,
  idUser: Option[IdentityId],
  cmp: Option[String],
  intCmp: Option[String],
  refererPageviewId: Option[String],
  refererUrl: Option[String],
  ophanPageviewId: Option[String],
  ophanBrowserId: Option[String],
  componentId: Option[String],
  componentType: Option[ComponentType],
  source: Option[AcquisitionSource],
  abTest: Option[AbTest]
)

object CaptureRequest {
  import utils.ThriftUtils.Implicits._ // Ignore IntelliJ - this is used!

  implicit val captureRequestReads: Reads[CaptureRequest] = Json.reads[CaptureRequest]
}
