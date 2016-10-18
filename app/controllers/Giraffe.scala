package controllers

import java.lang.Math.min
import java.time.LocalDate

import actions.CommonActions.NoCacheAction
import cats.data.XorT
import com.gu.i18n.CountryGroup._
import com.gu.i18n._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Charge
import com.gu.stripe.Stripe.Serializer._
import com.netaporter.uri.dsl._
import configuration.Config
import models.{ContributionId, IdentityId, SavedContributionData}
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import play.filters.csrf.CSRF
import play.filters.csrf.CSRFAddToken
import services.PaymentServices
import utils.MaxAmount
import utils.RequestCountry._
import views.support._

import scala.concurrent.Future

class Giraffe(paymentServices: PaymentServices, addToken: CSRFAddToken) extends Controller with Redirect {

  implicit val currencyFormatter = new Formatter[Currency] {
    type Result = Either[Seq[FormError], Currency]
    override def bind(key: String, data: Map[String, String]): Result =
      data.get(key).map(_.toUpperCase).flatMap(Currency.fromString).fold[Result](Left(Seq.empty))(currency => Right(currency))
    override def unbind(key: String, value: Currency): Map[String, String] =
      Map(key -> value.identifier)
  }

  case class JsonAbTest(testName: String, testSlug: String, variantName: String, variantSlug: String)

  object JsonAbTest {
    implicit val abTestFormat = Json.format[JsonAbTest]
  }

  case class SupportForm(
                          name: String,
                          currency: Currency,
                          amount: BigDecimal,
                          email: String,
                          token: String,
                          marketing: Boolean,
                          postcode: Option[String],
                          abTests: Set[JsonAbTest],
                          ophanPageviewId: String,
                          ophanBrowserId: Option[String],
                          cmp: Option[String],
                          intcmp: Option[String]

                        )

  val supportForm: Form[SupportForm] = Form(
    mapping(
      "name" -> text,
      "currency" -> of[Currency],
      "amount" -> bigDecimal(10, 2),
      "email" -> email,
      "token" -> nonEmptyText,
      "marketing" -> boolean,
      "postcode" -> optional(nonEmptyText),
      "abTests" -> set(mapping(
        "testName" -> text,
        "testSlug" -> text,
        "variantName" -> text,
        "variantSlug" -> text
      )(JsonAbTest.apply)(JsonAbTest.unapply)),
      "ophanPageviewId" -> text,
      "ophanBrowserId" -> optional(text),
      "cmp" -> optional(text),
      "intcmp" -> optional(text)
    )(SupportForm.apply)(SupportForm.unapply)
  )


  val social: Set[Social] = Set(
    Twitter("I've just contributed to the Guardian. Join me in supporting independent journalism https://membership.theguardian.com/contribute"),
    Facebook("https://contribute.theguardian.com/?INTCMP=social")
  )

  val chargeId = "charge_id"

  def contributeRedirect = NoCacheAction { implicit request =>

    val countryGroup = request.getFastlyCountry match {
      case Some(Canada) | Some(NewZealand) | Some(RestOfTheWorld) => UK
      case Some(other) => other
      case None => UK
    }

    redirectWithQueryParams(routes.Giraffe.contribute(countryGroup).url)
  }

  def redirectToUk = NoCacheAction { implicit request => redirectWithQueryParams(routes.Giraffe.contribute(UK).url) }

  private def redirectWithQueryParams(destinationUrl: String)(implicit request: Request[Any]) = redirectWithCampaignCodes(destinationUrl, Set("mcopy", "skipAmount", "highlight"))

  def postPayment(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some("https://media.guim.co.uk/5719a2b724efd8944e0222d57c839a7d2b6e39b3/0_0_1440_864/1000.jpg"),
      stripePublicKey = None,
      description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )
    Ok(views.html.giraffe.postPayment(pageInfo, countryGroup))
  }

  def contribute(countryGroup: CountryGroup, error: Option[PaymentError] = None) = addToken {
    NoCacheAction { implicit request =>

      val mvtId = Test.testIdFor(request)
      val variant = Test.getContributePageVariant(countryGroup, mvtId, request)

      val errorMessage = error.map(_.message)
      val stripe = paymentServices.stripeServiceFor(request)
      val cmp = request.getQueryString("CMP")
      val intCmp = request.getQueryString("INTCMP")

      val pageInfo = PageInfo(
        title = "Support the Guardian | Contribute today",
        url = request.path,
        image = Some("https://media.guim.co.uk/5719a2b724efd8944e0222d57c839a7d2b6e39b3/0_0_1440_864/1000.jpg"),
        stripePublicKey = Some(stripe.publicKey),
        description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
        customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
      )

      val maxAmountInLocalCurrency = MaxAmount.forCurrency(countryGroup.currency)
      val creditCardExpiryYears = CreditCardExpiryYears(LocalDate.now.getYear, 10)

      val testsInCookies = request.cookies.filter(_.name.contains(Test.CookiePrefix)) map(_.name)

      Ok(views.html.giraffe.contribute(pageInfo, maxAmountInLocalCurrency, countryGroup, variant, cmp, intCmp,
        creditCardExpiryYears, errorMessage, CSRF.getToken.map(_.value)))
        .discardingCookies(testsInCookies.toSeq map(DiscardingCookie(_)): _*)
        .withCookies(Test.testIdCookie(mvtId), Test.variantCookie(variant))
    }
  }

  def thanks(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    val charge = request.session.get(chargeId)
    val title = "Thank you!"

    Ok(views.html.giraffe.thankyou(PageInfo(
      title = title,
      url = request.path,
      image = None,
      description = Some("You’ve made a vital contribution that will help us maintain our independent, investigative journalism")
    ), social, countryGroup, charge))
  }

  def pay = NoCacheAction.async(parse.form(supportForm)) { implicit request =>

    val form = request.body

    val stripe = paymentServices.stripeServiceFor(request)
    val idUser = IdentityId.fromRequest(request)

    val countryGroup = form.currency match {
      case USD => US
      case AUD => Australia
      case EUR => Europe
      case _ => UK
    }

    val variant = Test.getContributePageVariant(countryGroup, Test.testIdFor(request), request)

    val contributionId = ContributionId.random

    val metadata = Map(
      "marketing-opt-in" -> form.marketing.toString,
      "email" -> form.email,
      "name" -> form.name,
      "abTests" -> Json.toJson(Seq(variant)).toString,
      "ophanPageviewId" -> form.ophanPageviewId,
      "ophanBrowserId" -> form.ophanBrowserId.getOrElse(""),
      "cmp" -> form.cmp.mkString,
      "intcmp" -> form.intcmp.mkString,
      "contributionId" -> contributionId.toString
    ) ++ List(
      form.postcode.map("postcode" -> _),
      idUser.map("idUser" -> _.id)
    ).flatten.toMap
    // Note that '.. * 100' will not work for Yen and other currencies! https://stripe.com/docs/api#charge_object-amount
    val amountInSmallestCurrencyUnit = (form.amount * 100).toInt
    val maxAmountInSmallestCurrencyUnit = MaxAmount.forCurrency(form.currency) * 100
    val amount = min(maxAmountInSmallestCurrencyUnit, amountInSmallestCurrencyUnit)

    val redirect = routes.Giraffe.thanks(countryGroup).url

    def createCharge: Future[Stripe.Charge] = {
      stripe.Charge.create(amount, form.currency, form.email, "Your contribution", form.token, metadata)
    }

    def storeMetaData(charge: Charge): XorT[Future, String, SavedContributionData] = {
      stripe.storeMetaData(
        contributionId = contributionId,
        created = new DateTime(charge.created * 1000L),
        email = charge.receipt_email,
        name = form.name,
        postCode = form.postcode,
        marketing = form.marketing,
        variants = Seq(variant),
        cmp = form.cmp,
        intCmp = form.intcmp,
        ophanPageviewId = form.ophanPageviewId,
        ophanBrowserId = form.ophanBrowserId,
        idUser = idUser
      )
    }

    createCharge.map { charge =>
      storeMetaData(charge) // fire and forget. If it fails we don't want to stop the user
      Ok(Json.obj("redirect" -> redirect))
        .withSession(chargeId -> charge.id)
    }.recover {
      case e: Stripe.Error => BadRequest(Json.toJson(e))
    }
  }
}


object CreditCardExpiryYears {
  def apply(currentYear: Int, offset: Int): List[Int] = {
    val currentYearShortened = currentYear % 100
    val subsequentYears = (currentYearShortened to currentYearShortened + offset - 2) map { _ + 1}
    currentYearShortened :: subsequentYears.toList
  }
}
