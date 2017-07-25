package acceptance.util

import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.scalatest.selenium.WebBrowser

import scala.util.Try

import collection.JavaConverters._


trait Browser extends WebBrowser {

  implicit val driver = Driver()

  def pageHasElement(q: Query): Boolean =
    waitUntil(ExpectedConditions.visibilityOfElementLocated(q.by))

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

  private def waitUntil[T](pred: ExpectedCondition[T]): Boolean =
    Try(new WebDriverWait(driver, Config.waitTimeout).until(pred)).isSuccess

}
