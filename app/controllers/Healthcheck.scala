package controllers

import play.api.mvc.Controller

object Healthcheck extends Controller {
def healthcheck = Ok("OK")
}
