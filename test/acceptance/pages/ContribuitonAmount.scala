package acceptance.pages

import acceptance.util.Browser
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
}



object YourDetails extends Page
  with Browser {

  val url = s"$baseUrl/"
  
  def pageHasLoaded: Boolean = pageHasElement(payButton)

  private val payButton = className("action--pay")
}
