package services

import abtests.{Variant, Percentage, Test, Allocation}
import com.gu.monitoring.ServiceMetrics
import com.gu.okhttp.RequestRunners
import models.PaymentProvider
import org.scalatest.{MustMatchers, WordSpec}
import play.api.Environment
import scala.concurrent.ExecutionContext.Implicits.global

class OphanServiceSpec(environment: Environment) extends WordSpec with MustMatchers {

  val browserId = "browserId"
  val visitId = "visitId"
  val ophanMetrics: ServiceMetrics = new ServiceMetrics("test", "ophan", "tracker")

  val ophanService = new OphanService(RequestRunners.loggingRunner(ophanMetrics), environment)

  "Extract Ab test" must {
    "create expected output from data" in {
      val data = Set(
        Allocation(
          Test(name = "test",
            audienceSize = Percentage(100d),
            audienceOffset = Percentage(0d),
            variants = Seq(Variant("control"))
          ),
          variant = Variant("control")
        ),
        Allocation(
          Test(name = "test2",
            audienceSize = Percentage(100d),
            audienceOffset = Percentage(0d),
            variants = Seq(Variant("control"))
          ),
          variant = Variant("control")
        )
      )

      val result = OphanAcquisitionEvent.abTestToOphanJson(data)
      result mustBe """{"test":{"variantName":"control"},"test2":{"variantName":"control"}}"""

    }
  }

  "Ophan Request" must {
    "should be encoded correctly" in {

      val allocation = Allocation(
        Test(name = "test",
          audienceSize = Percentage(100d),
          audienceOffset = Percentage(0d),
          variants = Seq(Variant("control"))
        ),
        variant = Variant("control")
      )

      val ophanEvent = OphanAcquisitionEvent(
        viewId = "123",
        browserId = browserId,
        product = Contribution,
        paymentFrequency = PaymentFrequency.OneOff,
        currency = "GBP",
        amount = 10.12,
        visitId = Some("visit"),
        amountInGBP = Some(10.12),
        paymentProvider = Some(PaymentProvider.Stripe),
        campaignCode = Some(Set("woot","poot")),
        abTests = Set(allocation),
        countryCode = Some("US"),
        referrerPageViewId = Some("refpivd"),
        referrerUrl = Some("refurl")
      )

      val params = ophanEvent.toParams
      val url = ophanService.endpointUrl("a.gif", Seq(params:_*)).toString
      url mustBe """https://contribute.thegulocal.com/testophan/a.gif?viewId=123&browserId=browserId&product=CONTRIBUTION&currency=GBP&paymentFrequency=ONE_OFF&amount=10.12&visitId=Some%28visit%29&amountInGBP=10.12&paymentProvider=STRIPE&campaignCode=woot,poot&abTests=%7B%22test%22%3A%7B%22variantName%22%3A%22control%22%7D%7D&countryCode=US&referrerPageViewId=refpivd&referrerUrl=refurl"""

    }
  }

}


