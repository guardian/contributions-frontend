package configuration
import com.netaporter.uri.dsl._

import com.typesafe.config.ConfigFactory
import play.api.Logger

object Config {

  val logger = Logger(this.getClass)

  val config = ConfigFactory.load()
  val contributeUrl = config.getString("contribute.url")
  val facebookAppId = config.getString("facebook.app.id")
  val idWebAppUrl = config.getString("identity.webapp.url")
  val thankYouEmailQueue = config.getString("email.thankYou.queueName")

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"
  val stageDev: Boolean = stage == "DEV"
  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")
  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$contributeUrl$uri") & idSkipConfirmation
  private val idSkipConfirmation: (String, String) = "skipConfirmation" -> "true"

}
