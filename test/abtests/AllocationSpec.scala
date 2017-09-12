package abtests

import org.mockito.Mockito
import org.scalatestplus.play.PlaySpec

class AllocationSpec extends PlaySpec {

  "An allocation" should {

    "be able to be converted into an ab test successfully using the test name slug and variant name" in {

      val mockAllocation = Mockito.mock[Allocation](classOf[Allocation], Mockito.RETURNS_DEEP_STUBS)

      val testName = "testName"
      val variantName = "variantName"

      Mockito.when(mockAllocation.test.slug).thenReturn(testName)
      Mockito.when(mockAllocation.variant.name).thenReturn(variantName)

      import com.gu.acquisition.utils.AbTestConverter.ops._

      val test = mockAllocation.asAbTest

      test mustEqual ophan.thrift.event.AbTest(testName, variantName)
    }
  }
}
