package acceptance.util

import com.gu.lib.okhttpscala._
import okhttp3.OkHttpClient
import okhttp3.Request.Builder

import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._

import configuration.Config


object Dependencies {
  object Contributions extends Availability {
    val url = Config.contributeUrl
  }

  trait Availability {
    val url: String
    def isAvailable: Boolean = {
      val request = new Builder().url(url).build()
      Try(Await.result(client.execute(request), 30 second).isSuccessful).getOrElse(false)
    }
  }

  private val client = new OkHttpClient()

}
