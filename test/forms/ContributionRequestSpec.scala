package forms

import com.gu.i18n.GBP
import controllers.forms.ContributionRequest
import org.scalatestplus.play.PlaySpec

class ContributionRequestSpec extends PlaySpec {
  import ContributionRequest._

  val baseContributionRequest = ContributionRequest(
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
    abTest = None
  )

  val baseFormData: Map[String, String] =  {
    import baseContributionRequest._
    Map(
      "name" -> name,
      "currency" -> currency.toString,
      "amount" -> amount.toString,
      "email" -> email,
      "token" -> token,
      "marketing" -> marketing.toString,
      "ophanPageviewId" -> ophanPageviewId
    )
  }

  "A contribution form" should {

    "be able to parse data successfully if no optional fields are included" in {

      baseContributionRequest mustEqual contributionForm.bind(baseFormData).value.value
    }

    "be able to parse data successfully when component information is included" in {

      val request = baseContributionRequest.copy(
        componentId = Some("componentId"),
        componentType = Some(ophan.thrift.componentEvent.ComponentType.AcquisitionsEpic)
      )

      val formData = baseFormData +
        ("componentId" -> "componentId") +
        ("componentType" -> "ACQUISITIONS_EPIC")

      request mustEqual contributionForm.bind(formData).value.value
    }

    "be able to parse data successfully when the acquisition source information is included" in {

      val request = baseContributionRequest.copy(source = Some(ophan.thrift.event.AcquisitionSource.GuardianApps))

      val formData = baseFormData + ("source" -> "GUARDIAN_APPS")

      request mustEqual contributionForm.bind(formData).value.value
    }

    "be able to parse data successfully when ab test information is included" in {

      val request = baseContributionRequest.copy(abTest = Some(ophan.thrift.event.AbTest("name", "variant")))

      val formData = baseFormData + ("abTest" -> """{"name": "name", "variant": "variant"}""")

      request mustEqual contributionForm.bind(formData).value.value
    }
  }
}
