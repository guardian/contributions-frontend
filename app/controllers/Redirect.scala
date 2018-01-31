package controllers

import cats.syntax.show._
import models.ContributionAmount
import org.joda.time.LocalDate
import play.api.mvc.{Controller, Request}

trait Redirect {
  self: Controller =>
  def redirect(destinationUrl: String)(implicit request: Request[Any]) = {
    Redirect(destinationUrl, SEE_OTHER)
  }

  def redirectWithQueryString(destinationUrl: String)(implicit request: Request[Any]) = {
    Redirect(destinationUrl, request.queryString, SEE_OTHER)
  }

  def mobileRedirectUrl(amount: ContributionAmount): String = {
    s"x-gu://contribution?date=${LocalDate.now().toString}&amount=${amount.show}"
  }
}
