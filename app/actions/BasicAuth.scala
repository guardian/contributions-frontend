package actions

import java.util.Base64

import play.api.mvc.{Action, Request, Result, Results}

import scala.concurrent.Future
import scala.util.Try

case class BasicAuth[A](user: String, password: String, realm: String = "Secured")(action: Action[A]) extends Action[A] {

  lazy val parser = action.parser

  def apply(request: Request[A]): Future[Result] = {
    BasicAuth.parseAuthorisation(request) match {
      case Some(creds) if creds.user == user && creds.password == password => action(request)
      case _ => Future.successful(Results.Unauthorized.withHeaders("WWW-Authenticate" -> s"""Basic realm="$realm""""))
    }
  }
}

object BasicAuth {
  case class Credentials(user: String, password: String)

  def splitAuth(authString: String): Option[Credentials] = {
    new String(authString).split(":").toList match {
      case parsedUser :: parsedPassword :: Nil => Some(Credentials(parsedUser, parsedPassword))
      case _ => None
    }
  }

  def parseAuthorisation[A](request: Request[A]): Option[Credentials] = {
    for {
      header <- request.headers.get("Authorization")
      encodedBlurb <- header.split(" ").drop(1).headOption
      decodedBlurb <- Try(new String(Base64.getDecoder.decode(encodedBlurb))).toOption
      credentials <- splitAuth(decodedBlurb)
    } yield credentials
  }
}
