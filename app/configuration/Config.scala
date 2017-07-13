package configuration
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory

import scala.util.Try

object Config {

  val config = ConfigFactory.load()
  val contributeUrl = config.getString("contribute.url")
  val domain = Uri.parse(contributeUrl).host
  val facebookAppId = config.getString("facebook.app.id")
  val idWebAppUrl = config.getString("identity.webapp.url")
  val thankYouEmailQueue = config.getString("email.thankYou.queueName")

  val appName = "contributions-frontend"

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"
  val stageDev: Boolean = stage == "DEV"
  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")
  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$contributeUrl$uri") & idSkipConfirmation
  private val idSkipConfirmation: (String, String) = "skipConfirmation" -> "true"

  object Logstash {
    private val param = Try {
      config.getConfig("param.logstash")
    }.toOption
    val stream = Try {
      param.map(_.getString("stream"))
    }.toOption.flatten
    val streamRegion = Try {
      param.map(_.getString("streamRegion"))
    }.toOption.flatten
    val enabled = Try {
      config.getBoolean("logstash.enabled")
    }.toOption.contains(true)
  }

  val guardianDomain = ".theguardian.com"
}
