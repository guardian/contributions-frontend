package services

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.memsub.util.Timing
import com.gu.memsub.Address
import configuration.Config
import controllers.IdentityRequest
import monitoring.IdentityApiMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
import views.support.IdentityUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import dispatch._, Defaults.timer



case class IdentityServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

case class IdentityService(identityApi: IdentityApi) {
  def getIdentityUserView(user: IdMinimalUser, identityRequest: IdentityRequest): Future[IdentityUser] =
    getFullUserDetails(user)(identityRequest)
      .zip(doesUserPasswordExist(identityRequest))
      .map { case (fullUser, doesPasswordExist) =>
        IdentityUser(fullUser, doesPasswordExist)
      }

  def getFullUserDetails(user: IdMinimalUser)(implicit identityRequest: IdentityRequest): Future[IdUser] =
    retry.Backoff(max = 3, delay = 2.seconds, base = 2){ () =>
      identityApi.get(s"user/${user.id}", identityRequest.headers, identityRequest.trackingParameters)
    }.map(_.getOrElse{
      val guCookieExists = identityRequest.headers.exists(_._1 == "X-GU-ID-FOWARDED-SC-GU-U")
      val guTokenExists = identityRequest.headers.exists(_._1 == "Authorization")
      val errContext = s"SC_GU_U=$guCookieExists GU-IdentityToken=$guTokenExists trackingParamters=${identityRequest.trackingParameters.toString}"
      throw IdentityServiceError(s"Couldn't get user's ${user.id} full details. $errContext")
    })

  def doesUserPasswordExist(identityRequest: IdentityRequest): Future[Boolean] =
    identityApi.getUserPasswordExists(identityRequest.headers, identityRequest.trackingParameters)

}

trait IdentityApi {

  def getUserPasswordExists(headers:List[(String, String)], parameters: List[(String, String)]) : Future[Boolean] = {
    val endpoint = "user/password-exists"
    val url = s"${Config.idApiUrl}/$endpoint"
    Timing.record(IdentityApiMetrics, "get-user-password-exists") {
      WS.url(url).withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(Duration(1000, "millis")).get().map { response =>
        recordAndLogResponse(response.status, "GET user-password-exists", endpoint)
        (response.json \ "passwordExists").asOpt[Boolean].getOrElse(throw new IdentityApiError(s"$url did not return a boolean"))
      }
    }
  }

  def get(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]) : Future[Option[IdUser]] = {
    Timing.record(IdentityApiMetrics, "get-user") {
      val url = s"${Config.idApiUrl}/$endpoint"
      WS.url(url).withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(Duration(1000, "millis")).get()
        .recover { case e =>
          Logger.error("Failure trying to retrieve user data", e)
          throw e
        }
        .map { response =>
          recordAndLogResponse(response.status, "GET user", endpoint)
          val jsResult = (response.json \ "user").validate[IdUser]
          if (jsResult.isError) Logger.error(s"Id Api response on $url : ${response.json.toString}")
          jsResult.asOpt
        }
        .recover { case e =>
          Logger.error("Failure trying to deserialise user data", e)
          None
        }
    }
  }

  def post(endpoint: String, data: Option[JsObject], headers: List[(String, String)], parameters: List[(String, String)], metricName: String): Future[Int] = {
    Timing.record(IdentityApiMetrics, metricName) {
      val requestHolder = WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(Duration(1000, "millis"))
      val response = requestHolder.post(data.getOrElse(JsNull))
      response.foreach(r => recordAndLogResponse(r.status, s"POST $metricName", endpoint ))
      response.map(_.status)
        .andThen {
          case Success(status) =>
            if ((status / 100) != 2) // non 2xx code
              Logger.error(s"Identity API error: POST ${Config.idApiUrl}/$endpoint STATUS $status")

          case Failure(e) =>
            Logger.error(s"Identity API error: POST ${Config.idApiUrl}/$endpoint", e)
        }
    }
  }



  private def recordAndLogResponse(status: Int, responseMethod: String, endpoint: String) {
    Logger.info(s"$responseMethod response $status for endpoint $endpoint")
    IdentityApiMetrics.putResponseCode(status, responseMethod)
  }
}

object IdentityApi extends IdentityApi

case class IdentityApiError(s: String) extends Throwable {
  override def getMessage: String = s
}
