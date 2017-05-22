package services

import com.typesafe.config.Config
import models.{Autofill, IdentityId}
import monitoring.LoggingTags
import monitoring.TagAwareLogger
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.{ExecutionContext, Future}

class IdentityService(wsClient: WSClient, config: Config)(implicit ec: ExecutionContext) extends TagAwareLogger {
  private val identityUrl = config.getString("apiUrl")
  private val token = config.getString("token")

  def request(path: String): WSRequest = {
    wsClient.url(s"$identityUrl/$path")
      .withHeaders("Authorization" -> s"Bearer $token")
  }

  def updateMarketingPreferences(userId: IdentityId, marketing: Boolean)(implicit tags: LoggingTags): Future[Boolean] = {
    val payload = Json.obj("statusFields" -> Json.obj("receiveGnmMarketing" -> marketing))
    request(s"user/${userId.id}").post(payload).map { response =>
      response.status >= 200 && response.status < 300
    } recover {
      case e: Exception =>
        error("Impossible to update the user's preferences", e)
        false
    }
  }

  def concatNames(first: Option[String], second: Option[String]): Option[String] =
    for {
      firstName <- first
      secondName <- second
    } yield s"$firstName $secondName"

  def makeUserRequest(cookie: String)(implicit tags: LoggingTags): Future[WSResponse] =
    request("user/me")
      .withHeaders("X-GU-ID-FOWARDED-SC-GU-U" -> cookie)
      .withHeaders("X-GU-ID-Client-Access-Token" -> s"Bearer $token")
      .get()

  def parseUserResponse(json: JsValue)(implicit tags: LoggingTags): Autofill =
    (for {
      user <- (json \ "user").validate[JsValue]
      email <- (user \ "primaryEmailAddress").validateOpt[String]
      firstName <- (user \ "privateFields" \ "firstName").validateOpt[String]
      secondName <- (user \ "privateFields" \ "secondName").validateOpt[String]
    } yield Autofill(concatNames(firstName, secondName), email)).getOrElse {
      error(s"Unable to parse json returned from Identity API to an autofill instance: ${Json.stringify(json)}")
      Autofill.empty
    }

  def autofill(cookie: String)(implicit tags: LoggingTags): Future[Autofill] =
    makeUserRequest(cookie)
      .map {
        case response if response.status == Status.OK =>
          parseUserResponse(response.json)
        case response =>
          error(s"Status ${response.status} returned from Identity API when getting user information")
          Autofill.empty
      }
      .recover {
        case err =>
          error("Error accessing Identity API", err)
          Autofill.empty
      }
}
