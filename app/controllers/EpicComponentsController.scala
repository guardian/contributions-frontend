package controllers

import actions.CommonActions.NoCacheAction
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller

class EpicComponentsController extends Controller {

  def inlinePayment: Action[AnyContent] = NoCacheAction { implicit request =>
    Ok(views.html.epicComponents.inlinePayment())
  }
}
