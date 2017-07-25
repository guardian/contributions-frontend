package acceptance.util

import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.scalatest.selenium.WebBrowser
import org.scalatest.selenium.WebBrowser.{Query, click}
import test.ManagedWebDriverFactory

import scala.util.Try


trait Browser extends WebBrowser with ManagedWebDriverFactory{
  def pageHasElement(q: Query): Boolean =
    waitUntil(ExpectedConditions.visibilityOfElementLocated(q.by))

  def clickOn(q: Query) {
    if (pageHasElement(q))
      click.on(q)
    else
      throw new Exception (s"Could not find query string")
  }

  private def waitUntil[T](pred: ExpectedCondition[T])(implicit webDriver: WebDriver): Boolean =
    Try(new WebDriverWait(webDriver, Config.waitTimeout).until(pred)).isSuccess

}
