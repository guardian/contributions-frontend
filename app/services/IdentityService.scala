package services

import com.typesafe.config.Config
import models.IdentityId
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.{ExecutionContext, Future}

class IdentityService(wsClient: WSClient, config: Config)(implicit ec: ExecutionContext) {
  val identityUrl = config.getString("apiUrl")
  val token = config.getString("token")

  def request(path: String): WSRequest = {
    wsClient.url(s"$identityUrl/$path")
      .withHeaders("Authorization" -> s"Bearer $token")
  }

  def updateMarketingPreferences(userId: IdentityId, marketing: Boolean): Future[Boolean] = {
    val payload = Json.obj("statusFields" -> Json.obj("receiveGnmMarketing" -> marketing))
    request(s"user/${userId.id}").post(payload).map { response =>
      response.status >= 200 && response.status < 300
    } recover {
      case e: Exception =>
        Logger.error("Impossible to update the user's preferences", e)
        false
    }
  }
}
