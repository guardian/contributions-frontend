package actions

import controllers.{Cached, NoCache}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CommonActions {

  val NoCacheAction = resultModifier(NoCache(_))

  val CachedAction = resultModifier(Cached(_))

  private def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }

  def SharedSecretAction[A](secret: String, parameterName: String = "api-key")(action: Action[A]) = new Action[A] {
    override def parser: BodyParser[A] = action.parser

    override def apply(request: Request[A]): Future[Result] = {
      if (request.getQueryString(parameterName).contains(secret)) {
        action.apply(request)
      } else {
        Future.successful(Results.Forbidden)
      }
    }
  }
}
