package controllers

import play.api.mvc.{Controller, Request}

trait Redirect {
  self: Controller =>
  def redirectWithCampaignCodes(destinationUrl: String, additionalParams: Set[String] = Set.empty)(implicit request: Request[Any]) = {
    val queryParamsToForward = Set("INTCMP", "CMP", "REFPVID", "app_id", "platform", "app_version") ++ additionalParams
    Redirect(destinationUrl, request.queryString.filterKeys(queryParamsToForward), SEE_OTHER)
  }
}
