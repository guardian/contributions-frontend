package acceptance.pages

import acceptance.util.{Browser, Config}
import acceptance.util.Config._
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.Page


object ContributionAmount extends Page
  with Browser {

  val url = s"$baseUrl/"

  def selectAmountButton(buttonNumber: Int): Unit =
    clickOnNth(className("contribute-controls__button"), buttonNumber)

  def payWithCard =
    clickOn(className("contribute_card__button"))

  def payWithPaypal =
    clickOn(className("paypal__button"))

}



object YourDetails extends Page
  with Browser {

  val url = s"$baseUrl/"

  val fullName = id("name")
  val email = id("email")
  val postcode = id("postcode")

  def fillInDetails() = {
    setValue(fullName, "Mx Testy McTestio")
    setValue(email, "test123@gu.vom")
    setValue(postcode, "TES T10")
  }

  def pay = clickOn(payButton)

  def pageHasLoaded: Boolean = pageHasElement(payButton)

  private val payButton = className("action--pay")
}

object StripeCheckout extends Page
  with Browser {
  val url = s"$baseUrl/"

  val container = name("stripe_checkout_app")
  val cardNumber = xpath("//div[label/text() = \"Card number\"]/input")
  val cardExp = xpath("//div[label/text() = \"Expiry\"]/input")
  val cardCvc = xpath("//div[label/text() = \"CVC\"]/input")
  val submitButton = xpath("//div[button]")

  def fillInCardDetails(cardNum: String) = {
    setValueSlowly(cardNumber, cardNum)
    setValueSlowly(cardExp, "1019")
    setValueSlowly(cardCvc, "111")
  }
  def acceptPayment(): Unit = clickOn(submitButton)
  def switchToStripe() = switchFrame(container)

  def pageHasLoaded: Boolean = pageHasElement(container)
}

object PaypalCheckout extends Page
  with Browser {
  val url = s"$baseUrl/"
  val paypalHeader = className("paypalHeader")
  val loginButton = name("btnLogin")
  val emailInput = name("login_email")
  val passwordInput = name("login_password")
  val agreeAndPay = id("confirmButtonTop")
  val paymentAmount = className("formatCurrency")
  val container = name("injectedUl")


  def canLogin: Boolean = pageHasElement(emailInput) && pageHasElement(passwordInput)

  def fillIn() = {
    setValueSlowly(emailInput, Config.paypalEmail)
    setValueSlowly(passwordInput, Config.paypalPassword)
  }

  def logIn() = clickOn(loginButton)
  def acceptPayment() = clickOn(agreeAndPay)
  def payPalHasPaymentSummary() = {
    pageDoesNotHaveElement(id("spinner"))
    pageHasElement(agreeAndPay)
  }
  def acceptPayPalPayment() = {
    pageDoesNotHaveElement(id("spinner"))
    acceptPayment
  }
  def switchToPayPal() = switchFrame(container)
}




object ThankYou extends Page
  with Browser {
  val url = s"$baseUrl/thankyou"

  def pageHasLoaded: Boolean = {
    // ensure that we are looking at the main page, and not the Stripe/Paypal iframe that may have just closed
    driver.switchTo().defaultContent()
    pageHasElement(className("thanks--description"))
  }

}

object PostPayment extends Page with Browser {
  val url = s"$baseUrl/post-payment"

  val next = className("action--next")
  val postPaymentConfirmation = className("postPaymentConfirmation")
  def pageHasLoaded: Boolean = pageHasElement(postPaymentConfirmation)
  def clickNext = clickOn(next)
}




