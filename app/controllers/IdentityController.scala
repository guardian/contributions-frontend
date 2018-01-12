package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup

import models._
import monitoring._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import services.IdentityService
import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.{ExecutionContext, Future}

class IdentityController(identityService: IdentityService)(implicit ec: ExecutionContext)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {
  val metadataUpdateForm = MarketingOptInUpdate.marketingOptInForm

  def updateMarketingPreferences(countryGroup: CountryGroup) = NoCacheAction(parse.form(metadataUpdateForm)) { implicit request =>
    val marketingOptIn = request.body.marketingOptIn

    request.session.data.get("email") match {
      case Some(email) => identityService.updateMarketingOptIn(email, marketingOptIn)
      case None => Future.successful(error("email not found in session while trying to update marketing opt in"))
    }

    val url = request.session.get("amount").flatMap(ContributionAmount.apply)
      .filter(_ => request.isAndroid)
      .map(mobileRedirectUrl)
      .getOrElse(routes.Contributions.thanks(countryGroup).url)

    Redirect(url, SEE_OTHER)
  }
}

case class MarketingOptInUpdate(marketingOptIn: Boolean)

object MarketingOptInUpdate {

  val marketingOptInForm: Form[MarketingOptInUpdate] = Form(
    mapping(
      "marketingOptIn" -> boolean
    )(MarketingOptInUpdate.apply)(MarketingOptInUpdate.unapply)
  )
}

