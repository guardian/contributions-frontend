package acceptance

import acceptance.util.{Browser, Dependencies, Driver}
import fixtures.TestApplicationFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.{BaseOneServerPerSuite, OneBrowserPerSuite, PlaySpec}



class StripeCheckoutSpec extends PlaySpec
  with TestApplicationFactory
  with BaseOneServerPerSuite
  with Browser
  with BeforeAndAfter
  with BeforeAndAfterAll {

  before { Driver.reset() }

  override protected def afterAll(): Unit = Driver.quit()

  private def checkDependenciesAreAvailable = {
    assume(Dependencies.Contributions.isAvailable,
      s"- ${Dependencies.Contributions.url} unavaliable! " +
        "\nPlease run contributions-frontend server before running tests.")
  }

  val contributionAmount = pages.ContributionAmount
  val yourDetails = pages.YourDetails

  "The OneBrowserPerTest trait" must {
    "allow end to end card payment" in {
      checkDependenciesAreAvailable
      go to contributionAmount
      contributionAmount.selectAmountButton(0)
      contributionAmount.payWithCard
      assert(yourDetails.pageHasLoaded)
    }
  }


}
