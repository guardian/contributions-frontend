package acceptance

import acceptance.util.{Browser, Dependencies}
import fixtures.TestApplicationFactory
import org.scalatestplus.play.{BaseOneServerPerSuite, OneBrowserPerSuite, PlaySpec}



class StripeCheckoutSpec extends PlaySpec
  with TestApplicationFactory
  with BaseOneServerPerSuite
  with OneBrowserPerSuite
  with Browser
  {


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
      pageTitle mustBe "Support the Guardian | Contribute today"
      contributionAmount.clickDebitCard
      assert(yourDetails.pageHasLoaded)
    }
  }


}
