package acceptance.util

import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.scalatest.selenium.WebBrowser

import scala.util.Try

import collection.JavaConverters._


trait Browser extends WebBrowser {

  implicit val driver = Driver()

  def pageHasElement(q: Query): Boolean =
    waitUntil(ExpectedConditions.visibilityOfElementLocated(q.by))

  def pageDoesNotHaveElement(q: Query): Boolean =
    waitUntil(ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(q.by)))

  def clickOn(q: Query) {
    if (pageHasElement(q))
      click.on(q)
    else
      throw new Exception (s"Could not find query string ${q.queryString}")
  }

  def clickOnNth(q: Query, n: Int): Unit = {
    if (pageHasElement(q)) {
      val element = driver.findElements(q.by).asScala.toList.lift(n)

      if (element.nonEmpty) element.foreach(_.click)
      else throw new Exception(s"Could not find ${n}th element for query ${q.queryString}")
    }
  }


  def setValue(q: Query, value: String, clear: Boolean = false) {
    if (pageHasElement(q)) {

      if (clear) q.webElement.clear
      q.webElement.sendKeys(value)

    } else
      throw new Exception(s"Could not fill in details for query ${q.queryString}")
  }

  // Switches to a new iframe specified by the Query, q.
  def switchFrame(q: Query) {
    if (pageHasElement(q))
      driver.switchTo().frame(q.webElement)
    else
      throw new Exception(s"Could not switch to the frame specified by the following query ${q.queryString}")
  }

  /*
  * Stripe wants you to pause between month and year and between each quartet in the cc
  * This causes pain when you use Selenium. There are a few stack overflow posts- but nothing really useful.
  * This pausing also seems to be necessary to make PayPal work properly,
  * without this it starts to complain about invalid credentials after only
  * a few characters.
  * */
  def setValueSlowly(q: Query, value: String): Unit = {
    for {
      c <- value
    } yield {
      setValue(q, c.toString)
      Thread.sleep(100)
    }
  }

  private def waitUntil[T](pred: ExpectedCondition[T]): Boolean =
    Try(new WebDriverWait(driver, Config.waitTimeout).until(pred)).isSuccess

}
