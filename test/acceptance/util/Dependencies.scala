package acceptance.util

import com.gu.lib.okhttpscala._
import okhttp3.OkHttpClient
import okhttp3.Request.Builder

import scala.concurrent.Await
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.Try

object Dependencies {
  object Contributions extends Availability {
    val url = Config.baseUrl
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
