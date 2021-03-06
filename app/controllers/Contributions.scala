package controllers

import java.time.LocalDate

import abtests.Test
import actions.CommonActions._
import com.gu.i18n.CountryGroup._
import com.gu.i18n._
import com.netaporter.uri.dsl._
import configuration.Config
import models.ReferrerAcquisitionData
import models.{ContributionAmount}
import monitoring.{CloudWatchMetrics, LoggingTagsProvider, TagAwareLogger}
import play.api.mvc._
import play.filters.csrf.{CSRF, CSRFAddToken}
import services.PaymentServices
import utils.MaxAmount
import utils.FastlyUtils._
import views.support._

import scala.util.Try

class Contributions(paymentServices: PaymentServices, addToken: CSRFAddToken, cloudWatchMetrics: CloudWatchMetrics)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {
  import ContributionsController._

  val social: Set[Social] = Set(
    Twitter("I've just contributed to the Guardian. Join me in supporting independent journalism https://membership.theguardian.com/contribute"),
    Facebook("https://contribute.theguardian.com/?INTCMP=social")
  )

  private def getPath(countryGroup: Option[CountryGroup]) = {
    countryGroup match {
      case Some(UK) => "uk"
      case Some(US) => "us"
      case Some(Australia) => "au"
      case Some(Canada) => "ca"
      case Some(NewZealand) => "nz"
      case Some(RestOfTheWorld) => "int"
      case Some(Europe) => "eu"
      case _ => "uk"
    }
  }

  def contributeRedirect = (NoCacheAction andThen MobileSupportAction) { implicit request =>
    val path = getPath(request.getFastlyCountryGroup)
    redirectWithQueryParams(s"https://support.theguardian.com/${path}/contribute")
  }

  private def redirectWithQueryParams(destinationUrl: String)(implicit request: Request[Any]) =
    redirectWithQueryString(destinationUrl)

  def postPayment(countryGroup: CountryGroup) = NoCacheAction.andThen(MetaDataAction.default) { implicit request =>
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some(Asset.absoluteUrl("images/twitter-card.png")),
      description = Some("By making a contribution, you’ll be supporting independent journalism that speaks truth to power"),
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )
    info(s"Paypal post-payment page displayed for contributions session id: ${request.sessionId}, platform: ${request.platform}.")
    cloudWatchMetrics.logPostPaymentPageDisplayed(request.paymentProvider, request.platform)
    Ok(views.html.giraffe.postPayment(pageInfo, countryGroup))
  }

  def contribute(countryGroup: CountryGroup, error: Option[PaymentError] = None) = addToken {
    NoCacheAction.andThen(MobileSupportAction).andThen(MetaDataAction.default) { implicit request =>
      val path = getPath(Some(countryGroup))
      redirectWithQueryParams(s"https://support.theguardian.com/${path}/contribute")
    }
  }

  def thanks(countryGroup: CountryGroup) = NoCacheAction.andThen(MetaDataAction.default) { implicit request =>
    val charge = request.session.get("charge_id")
    val title = "Thank you!"

    val iosRedirectUrl = request.session.get("amount")
      .flatMap(ContributionAmount.apply)
      .map(mobileRedirectUrl)
      .filter(_ => request.isIos)

    info(s"Thank you page displayed. contributions session id: ${request.sessionId}, platform: ${request.platform}. Payment method used was: ${request.paymentProvider.getOrElse("unknown")}.")
    cloudWatchMetrics.logThankYouPageDisplayed(request.paymentProvider, request.platform)

    Ok(views.html.giraffe.thankyou(PageInfo(
      title = title,
      url = request.path,
      image = None,
      description = Some("You’ve made a vital contribution that will help us maintain our independent, investigative journalism")
    ), social, countryGroup, charge, iosRedirectUrl))
  }
}

object ContributionsController extends TagAwareLogger with LoggingTagsProvider {

  def referrerAcquisitionDataFromRequest(implicit request: MetaDataRequest[_]): Option[ReferrerAcquisitionData] = {
    import cats.syntax.either._

    ReferrerAcquisitionData.fromQueryString(request.queryString)
      // When mobile starts sending acquisition data we will want to warn in all cases.
      .leftMap(err => if (!request.isMobile) {
        val userAgent = request.headers.get("User-Agent").getOrElse("unknown")
        val referrerUrl = request.headers.get("referer").getOrElse("unknown")
        warn(s"$err - User-Agent: $userAgent - referer: $referrerUrl - contributions session id: ${request.sessionId}")
      })
      .toOption
  }
}

object CreditCardExpiryYears {
  def apply(currentYear: Int, offset: Int): List[Int] = {
    val currentYearShortened = currentYear % 100
    val subsequentYears = (currentYearShortened to currentYearShortened + offset - 2) map { _ + 1}
    currentYearShortened :: subsequentYears.toList
  }
}
