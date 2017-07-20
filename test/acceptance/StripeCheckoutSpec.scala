package test

import acceptance.util.Dependencies
import fixtures.TestApplicationFactory
import org.scalatestplus.play.{BaseOneServerPerSuite, OneBrowserPerSuite, PlaySpec}



class StripeCheckoutSpec extends PlaySpec
  with TestApplicationFactory
  with BaseOneServerPerSuite
  with OneBrowserPerSuite
  with ManagedWebDriverFactory  {


  private def checkDependenciesAreAvailable = {
    assume(Dependencies.Contributions.isAvailable,
      s"- ${Dependencies.Contributions.url} unavaliable! " +
        "\nPlease run contributions-frontend server before running tests.")
  }


  "The OneBrowserPerTest trait" must {
    "provide a web driver" in {
      checkDependenciesAreAvailable
      go to Dependencies.Contributions.url
      println(pageTitle)
    }
  }


}
