package controllers

import models.ContributionAmount
import org.joda.time.LocalDate
import play.api.mvc.{Controller, Request}

trait Redirect {
  self: Controller =>
  def redirectWithCampaignCodes(destinationUrl: String, additionalParams: Set[String] = Set.empty)(implicit request: Request[Any]) = {
    val queryParamsToForward = Set("INTCMP", "CMP", "REFPVID", "platform") ++ additionalParams
    Redirect(destinationUrl, request.queryString.filterKeys(queryParamsToForward), SEE_OTHER)
  }

  def thankYouMobileUri(amount: ContributionAmount): String = {
    s"x-gu://contribution?date=${LocalDate.now().toString}&amount=$amount"
  }
}
