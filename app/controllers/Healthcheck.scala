package controllers

import play.api.mvc.{Action, Controller}

class Healthcheck extends Controller {
  def healthcheck() = Action{
    Cached(1)(Ok("yeah its all good"))
  }
}
