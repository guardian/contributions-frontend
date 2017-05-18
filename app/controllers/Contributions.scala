package controllers

import java.time.LocalDate

import abtests.Test
import actions.CommonActions._
import com.gu.i18n.CountryGroup._
import com.gu.i18n._
import com.netaporter.uri.dsl._
import configuration.Config
import models.ContributionAmount
import monitoring.TagAwareLogger
import play.api.mvc._
import play.filters.csrf.{CSRF, CSRFAddToken}
import services.PaymentServices
import utils.MaxAmount
import utils.RequestCountry._
import views.support._

class Contributions(paymentServices: PaymentServices, addToken: CSRFAddToken) extends Controller with Redirect {

  val social: Set[Social] = Set(
    Twitter("I've just contributed to the Guardian. Join me in supporting independent journalism https://membership.theguardian.com/contribute"),
    Facebook("https://contribute.theguardian.com/?INTCMP=social")
  )

  def contributeRedirect = (NoCacheAction andThen MobileSupportAction) { implicit request =>

    val countryGroup = request.getFastlyCountry match {
      case Some(Canada) | Some(NewZealand) | Some(RestOfTheWorld) => UK
      case Some(other) => other
      case None => UK
    }

    redirectWithQueryParams(routes.Contributions.contribute(countryGroup).url)
  }

  def redirectToUk = (NoCacheAction andThen MobileSupportAction) { implicit request => redirectWithQueryParams(routes.Contributions.contribute(UK).url) }

  private def redirectWithQueryParams(destinationUrl: String)(implicit request: Request[Any]) = redirectWithCampaignCodes(destinationUrl, Set("mcopy", "skipAmount", "highlight"))

  def postPayment(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some(Asset.absoluteUrl("images/twitter-card.png")),
      stripePublicKey = None,
      description = Some("By making a contribution, you’ll be supporting independent journalism that speaks truth to power"),
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )
    Ok(views.html.giraffe.postPayment(pageInfo, countryGroup))
  }

  def contribute(countryGroup: CountryGroup, error: Option[PaymentError] = None) = addToken {
    (NoCacheAction andThen MobileSupportAction andThen ABTestAction) { implicit request =>

      val errorMessage = error.map(_.message)
      val stripe = paymentServices.stripeServiceFor(request)
      val cmp = request.getQueryString("CMP")
      val intCmp = request.getQueryString("INTCMP")
      val refererPageviewId = request.getQueryString("REFPVID")
      val refererUrl = request.headers.get("referer")

      val pageInfo = PageInfo(
        title = "Support the Guardian | Contribute today",
        url = request.path,
        image = Some(Asset.absoluteUrl("images/twitter-card.png")),
        stripePublicKey = Some(stripe.publicKey),
        description = Some("By making a contribution, you’ll be supporting independent journalism that speaks truth to power"),
        customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
      )

      val maxAmountInLocalCurrency = MaxAmount.forCurrency(countryGroup.currency)
      val creditCardExpiryYears = CreditCardExpiryYears(LocalDate.now.getYear, 10)

      Ok(views.html.giraffe.contribute(
        pageInfo,
        maxAmountInLocalCurrency,
        countryGroup,
        request.testAllocations,
        cmp,
        intCmp,
        refererPageviewId,
        refererUrl,
        creditCardExpiryYears,
        errorMessage,
        CSRF.getToken.map(_.value),
        request.isAllocated(Test.landingPageTest, "with-copy")
      ))
    }
  }

  def thanks(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    val charge = request.session.get("charge_id")
    val title = "Thank you!"

    val iosRedirectUrl = request.session.get("amount")
      .flatMap(ContributionAmount.apply)
      .map(mobileRedirectUrl)
      .filter(_ => request.isIos)

    Ok(views.html.giraffe.thankyou(PageInfo(
      title = title,
      url = request.path,
      image = None,
      description = Some("You’ve made a vital contribution that will help us maintain our independent, investigative journalism")
    ), social, countryGroup, charge, iosRedirectUrl))
  }
}

object CreditCardExpiryYears {
  def apply(currentYear: Int, offset: Int): List[Int] = {
    val currentYearShortened = currentYear % 100
    val subsequentYears = (currentYearShortened to currentYearShortened + offset - 2) map { _ + 1}
    currentYearShortened :: subsequentYears.toList
  }
}
