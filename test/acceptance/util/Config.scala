package acceptance.util

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import scala.util.{Try, Failure, Success}

object Config {
  private def logger = LoggerFactory.getLogger(this.getClass)
  private val conf = ConfigFactory.load()

  val waitTimeout: Int = conf.getInt("waitTimeout")
  val baseUrl = conf.getString("contribute.url")

  val testUsersSecret = conf.getString("identity.test.users.secret")

  val webDriverRemoteUrl = Try(conf.getString("webDriverRemoteUrl")) match {
    case Success(url) => url
    case Failure(e) => ""
  }

  val screencastIdFile = conf.getString("screencastId.file")

  val paypalEmail = conf.getString("paypal.TEST.email")
  val paypalPassword = conf.getString("paypal.TEST.password")

  def debug() { conf.root().render() }
}
