package acceptance

import acceptance.util._
import fixtures.TestApplicationFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.play.{BaseOneServerPerSuite, PlaySpec}

class CheckoutAcceptanceTest extends PlaySpec
  with TestApplicationFactory
  with BaseOneServerPerSuite
  with Browser
  with TestUserGenerator
  with BeforeAndAfter
  with BeforeAndAfterAll {

  before {
    Driver.reset()
  }

  override def beforeAll() {
    Screencast.storeId()
  }

  override protected def afterAll(): Unit = Driver.quit()

  private def checkDependenciesAreAvailable = {
    assume(Dependencies.Contributions.isAvailable,
      s"- ${Dependencies.Contributions.url} unavaliable! " +
        "\nPlease run contributions-frontend server before running tests.")
  }

  val contributionAmount = pages.ContributionAmount
  val yourDetails = pages.YourDetails
  val stripeCheckout = pages.StripeCheckout
  val paypalCheckout = pages.PaypalCheckout
  val thankYou = pages.ThankYou
  val postPayment = pages.PostPayment

  "The contributions site" must {
    "allow card payments" in {
      checkDependenciesAreAvailable
      val username = addTestUserCookie

      go to contributionAmount
      contributionAmount.selectAmountButton(0)
      contributionAmount.payWithCard
      assert(yourDetails.pageHasLoaded)
      yourDetails.fillInDetails(username)
      yourDetails.pay
      assert(stripeCheckout.pageHasLoaded)
      stripeCheckout.switchToStripe
      stripeCheckout.fillInCardDetails("4242 4242 4242 4242")
      stripeCheckout.acceptPayment
      assert(thankYou.pageHasLoaded)
    }

    "allow PayPal payments" in {
      checkDependenciesAreAvailable
      addTestUserCookie

      go to contributionAmount
      contributionAmount.selectAmountButton(0)
      contributionAmount.payWithPaypal

      paypalCheckout.switchToPayPal
      assert(paypalCheckout.canLogin)
      paypalCheckout.fillIn
      paypalCheckout.logIn
      assert(paypalCheckout.payPalHasPaymentSummary)
      paypalCheckout.acceptPayPalPayment
      assert(postPayment.pageHasLoaded)
      postPayment.clickNext
      assert(thankYou.pageHasLoaded)
    }
  }
}
