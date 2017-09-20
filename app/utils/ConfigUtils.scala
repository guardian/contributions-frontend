package utils

import com.typesafe.config.Config

import scala.util.Try

object ConfigUtils {
  implicit class OptionalConfig(c: Config) {
    def getOptionalConfig(path: String): Option[Config] = Try(c.getConfig(path)).toOption
  }
}
