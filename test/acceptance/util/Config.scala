package acceptance.util

import com.typesafe.config.ConfigFactory

object Config {
  private val conf = ConfigFactory.load()

  val waitTimeout: Int = conf.getInt("waitTimeout")
  val baseUrl = conf.getString("contribute.url")

}
