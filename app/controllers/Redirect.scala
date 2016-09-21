package controllers

import play.api.mvc.{Controller, Request}

trait Redirect {
  self: Controller =>
  def redirectWithCampaignCodes(destinationUrl: String, additionalParams: Set[String] = Set.empty)(implicit request: Request[Any]) = {
    val queryParamsToForward = Set("INTCMP", "CMP") ++ additionalParams
    Redirect(destinationUrl, request.queryString.filterKeys(queryParamsToForward), SEE_OTHER)
  }
}
