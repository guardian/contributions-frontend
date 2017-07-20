package test
import io.github.bonigarcia.wdm.ChromeDriverManager
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.scalatestplus.play.BrowserFactory

trait ManagedWebDriverFactory extends BrowserFactory {
  override def createWebDriver(): WebDriver = {
    ChromeDriverManager.getInstance.setup
    new ChromeDriver()
  }
}
