package services

import com.typesafe.config.Config
import models.{Autofill, IdentityId}
import monitoring.LoggingTags
import monitoring.TagAwareLogger
import play.api.libs.json._
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

  def autofill(cookie: String)(implicit tags: LoggingTags): Future[Autofill] = {

    def name(optionalFirstName: Option[String], optionalSecondName: Option[String]): Option[String] = for {
      firstName <- optionalFirstName
      secondName <- optionalSecondName
    } yield s"$firstName $secondName"

    def parse(jsValue: JsValue)(implicit tags: LoggingTags): Autofill = {
      val result = for {
        status <- (jsValue \ "status").validate[String]
        user <- (jsValue \ "user").validate[JsValue]
        email <- (user \ "primaryEmailAddress").validateOpt[String]
        firstName <- (user \ "privateFields" \ "firstName").validateOpt[String]
        secondName <- (user \ "privateFields" \ "secondName").validateOpt[String]
      } yield {
        if (status == "ok") {
          Autofill(name(firstName, secondName), email)
        } else {
          error(s"Invalid identity API response status: $status")
          Autofill.empty
        }
      }
      result match {
        case JsSuccess(autofill, _) => autofill
        case JsError(e) =>
          error(s"Unable to parse json from identity $e")
          Autofill.empty
      }
    }

    readUser(cookie).map(parse)
  }


  def readUser(cookie: String)(implicit tags: LoggingTags): Future[JsValue] = {
    request("user/me")
      .withHeaders("X-GU-ID-FOWARDED-SC-GU-U" -> cookie)
      .withHeaders("X-GU-ID-Client-Access-Token" -> s"Bearer $token")
      .get()
      .map(_.json)
      .recover { case e: Exception =>
        error("Unable to fetch user from identity", e)
        JsObject(Seq.empty)
      }
  }
}
