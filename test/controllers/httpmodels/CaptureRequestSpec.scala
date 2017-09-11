package controllers.httpmodels

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}

class CaptureRequestSpec extends PlaySpec with EitherValues {
  import utils.ThriftUtils.Implicits._

  private val baseCaptureRequest = CaptureRequest(
    paymentId = "paymentId",
    platform = "platform",
    idUser = None,
    cmp = None,
    intCmp = None,
    refererPageviewId = None,
    refererUrl = None,
    ophanPageviewId = None,
    ophanBrowserId = None,
    componentId = None,
    componentType = None,
    source = None,
    abTest = None
  )

  private val baseJson = Json.obj(
    "paymentId" -> baseCaptureRequest.paymentId,
    "platform" -> baseCaptureRequest.platform
  )

  def checkDecoding(json: JsValue, request: CaptureRequest): Unit =
    json.validate[CaptureRequest].asEither.right.value mustEqual request

  "A contribution request" when {

    "it is encoded as JSON" should {

      "be able to be decoded when there are no optional fields" in {

        checkDecoding(baseJson, baseCaptureRequest)
      }

      "be able to be decoded when the component fields have been included" in {

        val json = baseJson ++ Json.obj(
          "componentId" -> Some("componentId"),
          "componentType" -> Some("ACQUISITIONS_EPIC")
        )

        val request = baseCaptureRequest.copy(
        componentId = Some("componentId"),
        componentType = Some(ComponentType.AcquisitionsEpic)
        )

        checkDecoding(json, request)
      }

      "be able to be decoded when the acquisition source field has been included" in {

        val json = baseJson ++ Json.obj("source" -> "GUARDIAN_WEB")

        val request = baseCaptureRequest.copy(source = Some(AcquisitionSource.GuardianWeb))

        checkDecoding(json, request)
      }

      "be able to be decoded when the AB test field has been included" in {

        val json = baseJson ++ Json.obj("abTest" -> Json.obj("name" -> "name", "variant" -> "variant"))

        val request = baseCaptureRequest.copy(abTest = Some(AbTest("name", "variant")))

        checkDecoding(json, request)
      }
    }
  }
}
