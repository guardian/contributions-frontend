package configuration

import com.typesafe.config.Config
import scala.collection.JavaConversions._

case class CorsConfig(allowedOrigins: List[String])

object CorsConfig {
  def from(config: Config) = CorsConfig(
    allowedOrigins = config.getStringList("allowedOrigins").toList
  )
}
