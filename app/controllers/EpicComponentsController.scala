package controllers

import actions.CommonActions.NoCacheAction
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller

import scala.concurrent.duration._

class EpicComponentsController extends Controller {

  def inlinePayment: Action[AnyContent] = Action { implicit request =>
    Cached(30.days.toSeconds.toInt)(Ok(views.html.epicComponents.inlinePayment()))
  }
}
