package forms

import com.gu.i18n.GBP
import controllers.forms.ContributionRequest
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}

class ContributionRequestSpec extends PlaySpec with EitherValues {
  import ContributionRequest._

  val baseRequest = ContributionRequest(
    name = "name",
    currency = GBP,
    amount = 10,
    email = "test@gmail.com",
    token = "token",
    marketing = true,
    postcode = None,
    ophanPageviewId = "pageviewId",
    ophanBrowserId = None,
    cmp = None,
    intcmp = None,
    refererPageviewId = None,
    refererUrl = None,
    idUser = None,
    platform = None,
    ophanVisitId = None,
    componentId = None,
    componentType = None,
    source = None,
    abTest = None,
    refererAbTest = None,
    nativeAbTests = None
  )

  val baseJson: JsObject =  Json.obj(
    "name" -> "name",
    "currency" -> "gbp",
    "amount" -> "10.0",
    "email" -> "test@gmail.com",
    "token" -> "token",
    "marketing" -> true,
    "ophanPageviewId" -> "pageviewId"
  )

  def checkJson(request: ContributionRequest, json: JsObject): Unit =
    request mustEqual json.validate[ContributionRequest].asEither.right.value

  "A contribution form" should {

    "be able to parse data successfully if no optional fields are included" in {

      checkJson(baseRequest, baseJson)
    }

    "be able to parse data successfully when component information is included" in {

      val request = baseRequest.copy(
        componentId = Some("componentId"),
        componentType = Some(ophan.thrift.componentEvent.ComponentType.AcquisitionsEpic)
      )

      val json = baseJson ++ Json.obj("componentId" -> "componentId", "componentType" -> "ACQUISITIONS_EPIC")

      checkJson(request, json)
    }

    "be able to parse data successfully when the acquisition source information is included" in {

      val request = baseRequest.copy(source = Some(ophan.thrift.event.AcquisitionSource.GuardianApps))

      val json = baseJson ++ Json.obj("source" -> "GUARDIAN_APPS")

      checkJson(request, json)
    }

    "be able to parse data successfully when ab test information is included" in {

      val request = baseRequest.copy(abTest = Some(ophan.thrift.event.AbTest("name", "variant")))

      val json = baseJson ++ Json.obj("abTest" -> Json.obj("name" -> "name", "variant" -> "variant"))

      checkJson(request, json)

      val request2 = baseRequest.copy(refererAbTest = Some(ophan.thrift.event.AbTest("name", "variant")))

      val json2 = baseJson ++ Json.obj("refererAbTest" -> Json.obj("name" -> "name", "variant" -> "variant"))

      checkJson(request2, json2)

      val request3 = baseRequest.copy(nativeAbTests = Some(Set(ophan.thrift.event.AbTest("name", "variant"))))

      val json3 = baseJson ++ Json.obj("nativeAbTests" -> Json.arr(Json.obj("name" -> "name", "variant" -> "variant")))

      checkJson(request3, json3)
    }
  }
}
