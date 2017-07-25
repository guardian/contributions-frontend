package acceptance.pages

import acceptance.util.Browser
import acceptance.util.Config._
import org.scalatest.selenium.Page


object ContributionAmount extends Page with Browser {
  val url = s"$baseUrl/"

  def clickDebitCard = clickOn(className("action action--button action--button--forward contribute-navigation__button action action--button contribute-navigation__next action--next contribute_card__button"))


}



object YourDetails extends Page with Browser {
  val url = s"$baseUrl/"
  def pageHasLoaded: Boolean = pageHasElement(payButton)

  private val payButton = className("action--pay")
}
