package monitoring

import javax.inject._

import com.gu.identity.play.AuthenticatedIdUser
import controllers.{Cached, NoCache}
import monitoring.SentryLogging._
import org.slf4j.MDC
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.core.SourceMapper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

@Singleton
class ErrorHandler @Inject()(
    identityAuthProvider: AuthenticatedIdUser.Provider,
    env: Environment,
    config: Configuration,
    sourceMapper: Option[SourceMapper],
    router: => Option[Router]
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with TagAwareLogger with LoggingTagsProvider with AcceptExtractors {

  override def logServerError(request: RequestHeader, usefulException: UsefulException) {
    try {
      for (identityUser <- identityAuthProvider(request)) MDC.put(UserIdentityId, identityUser.id)

      MDC.put(PlayErrorId, usefulException.id)

      super.logServerError(request, usefulException)

    } finally MDC.clear()
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] =
    super.onClientError(request, statusCode, message).map(Cached(_))

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(request match {
      case Accepts.Html() => NotFound(views.html.error404())
      case _ => NotFound(s"Not Found ${message}")
    })
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful(NoCache(request match {
      case Accepts.Html() => InternalServerError(views.html.error500(exception))
      case _ => InternalServerError(s"Internal Server Error (error id: ${exception.id})")
    }))


  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    // Implicit logging tags passed explicitly as it can not be derived implicitly (the request is not implicit).
    error(s"Bad request: $message")(loggingTagsFromRequestHeader(request))

    Future.successful(NoCache(request match {
      case Accepts.Html() => BadRequest(views.html.error400(request, message))
      case _ => BadRequest(message)
    }))
  }
}
