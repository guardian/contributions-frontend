package configuration

import com.typesafe.config.Config

case class SupportConfig(thankYouURL: String)

object SupportConfig {
  def from(config: Config) = SupportConfig(
    thankYouURL = config.getString("thank-you-url")
  )
}
