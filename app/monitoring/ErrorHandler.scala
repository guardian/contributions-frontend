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
                               ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def logServerError(request: RequestHeader, usefulException: UsefulException) {
    try {
      for (identityUser <- identityAuthProvider(request)) MDC.put(UserIdentityId, identityUser.id)

      MDC.put(PlayErrorId, usefulException.id)

      super.logServerError(request, usefulException)

    } finally MDC.clear()
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {
    super.onClientError(request, statusCode, message).map(Cached(_))
  }

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful(NoCache(InternalServerError(views.html.error500(exception))))

  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    logServerError(request, new PlayException("Bad request","A very bad request was received!"))
    Future.successful(NoCache(BadRequest(views.html.error400(request,message))))
  }
}
