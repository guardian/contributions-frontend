package controllers

import models.Autofill
import monitoring.TagAwareLogger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller, Result}
import services.IdentityService

import scala.concurrent.{ExecutionContext, Future}

class UserController(identityService: IdentityService)(implicit ec: ExecutionContext) extends Controller {

  def autofill: Action[AnyContent] = Action.async { implicit request =>

    def serialise(result: Autofill): Result = {
      NoCache(Ok(Json.toJson(result)))
    }

    request.cookies.get("SC_GU_U") match {
      case Some(cookie) => identityService.autofill(cookie.value).map(serialise)
      case None => Future.successful(serialise(Autofill.empty))
    }
  }
}
