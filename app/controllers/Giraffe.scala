package controllers

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import actions.CommonActions
import actions.CommonActions.NoCacheAction
import com.gu.i18n._
import com.gu.stripe.{Stripe, StripeService}
import com.gu.stripe.Stripe.Serializer._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.mvc._
import configuration.Config
import services.PaymentServices
import com.netaporter.uri.dsl._
import views.support.{TestTrait, _}

import scalaz.syntax.std.option._
import scala.concurrent.Future
import utils.RequestCountry._
import com.netaporter.uri.dsl._
import com.netaporter.uri.{PathPart, Uri}
import controllers._
import play.api.data.{FieldMapping, Form, FormError}
import play.api.data.Forms._
import play.api.data.format.Formatter
import java.time.LocalDate

class Giraffe(paymentServices: PaymentServices) extends Controller {
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
                          postCode: Option[String],
                          abTests: Set[JsonAbTest],
                          ophanId: String,
                          cmp: Option[String],
                          intcmp: Option[String]

                        )

  val supportForm: Form[SupportForm] = Form(
    mapping(
      "name" -> nonEmptyText,
      "currency" -> of[Currency],
      "amount" -> bigDecimal(10, 2),
      "email" -> email,
      "token" -> nonEmptyText,
      "guardian-opt-in" -> boolean,
      "postcode" -> optional(nonEmptyText),
      "abTests" -> set(mapping(
        "testName" -> text,
        "testSlug" -> text,
        "variantName" -> text,
        "variantSlug" -> text
      )(JsonAbTest.apply)(JsonAbTest.unapply)),
      "ophanId" -> text,
      "cmp" -> optional(text),
      "intcmp" -> optional(text)
    )(SupportForm.apply)(SupportForm.unapply)
  )


  val social: Set[Social] = Set(
    Twitter("I've just contributed to the Guardian. Join me in supporting independent journalism https://membership.theguardian.com/contribute"),
    Facebook("https://membership.theguardian.com/contribute")
  )


  val chargeId = "charge_id"

  def maxAmount(currency: Currency): Option[Int] = currency match {
    case CountryGroup.Australia.currency => 3500.some
    case _ => 2000.some
  }

  def contributeRedirect = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.RestOfTheWorld)
    val CampaignCodesToForward = Set("INTCMP", "CMP", "mcopy")

    Redirect(routes.Giraffe.contribute(countryGroup).url, request.queryString.filterKeys(CampaignCodesToForward), SEE_OTHER)
  }

  // Once things have settled down and we have a reasonable idea of what might
  // and might not vary between different countries, we should merge these country-specific
  // controllers & templates into a single one which varies on a number of parameters
  def contribute(countryGroup: CountryGroup, react: Boolean = false) = NoCacheAction { implicit request =>
    val stripe = paymentServices.stripeServiceFor(request)
    val cmp = request.getQueryString("CMP")
    val intCmp = request.getQueryString("INTCMP")
    val chosenVariants: ChosenVariants = Test.getContributePageVariants(countryGroup, request)
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some("https://media.guim.co.uk/5719a2b724efd8944e0222d57c839a7d2b6e39b3/0_0_1440_864/1000.jpg"),
      stripePublicKey = Some(stripe.publicKey),
      description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )

    val maxAmountInLocalCurrency = maxAmount(countryGroup.currency)
    val creditCardExpiryYears = CreditCardExpiryYears(LocalDate.now.getYear, 10)

    val template = {
      if (react) views.html.giraffe.contributeReact(pageInfo, maxAmountInLocalCurrency, countryGroup, chosenVariants, cmp, intCmp, creditCardExpiryYears)
      else views.html.giraffe.contribute(pageInfo, maxAmountInLocalCurrency, countryGroup, chosenVariants, cmp, intCmp, creditCardExpiryYears)
    }

    Ok(template).withCookies(Test.createCookie(chosenVariants.v1), Test.createCookie(chosenVariants.v2))
  }

  def contributeReact = contribute(CountryGroup.UK, true)

  def thanks(countryGroup: CountryGroup) = NoCacheAction { implicit request =>

    val title = countryGroup match {
      case CountryGroup.Australia => "Thank you for supporting Guardian Australia"
      case _ => "Thank you for supporting the Guardian"
    }

    val redirectUrl = routes.Giraffe.contribute(countryGroup).url
    Ok(views.html.giraffe.thankyou(PageInfo(
      title = title,
      url = request.path,
      image = None,
      description = Some("Your contribution is much appreciated, and will help us to maintain our independent, investigative journalism.")
    ), social, countryGroup))
  }

  def pay = NoCacheAction.async { implicit request =>
    val stripe = paymentServices.stripeServiceFor(request)

    supportForm.bindFromRequest().fold[Future[Result]]({ withErrors =>
      Future.successful(BadRequest(JsArray(withErrors.errors.map(k => JsString(k.key)))))
    }, { f =>
      val metadata = Map(
        "marketing-opt-in" -> f.marketing.toString,
        "email" -> f.email,
        "name" -> f.name,
        "abTests" -> Json.toJson(f.abTests).toString,
        "ophanId" -> f.ophanId,
        "cmp" -> f.cmp.mkString,
        "intcmp" -> f.intcmp.mkString
      ) ++ f.postCode.map("postcode" -> _)
      val res = stripe.Charge.create(maxAmount(f.currency).fold((f.amount * 100).toInt)(max => Math.min(max * 100, (f.amount * 100).toInt)), f.currency, f.email, "Your contribution", f.token, metadata)


      val redirect = f.currency match {
        case USD => routes.Giraffe.thanks(CountryGroup.US).url
        case AUD => routes.Giraffe.thanks(CountryGroup.Australia).url
        case EUR => routes.Giraffe.thanks(CountryGroup.Europe).url
        case _ => routes.Giraffe.thanks(CountryGroup.UK).url
      }

      res.map { charge =>
        Ok(Json.obj("redirect" -> redirect))
          .withSession(chargeId -> charge.id)
      }.recover {
        case e: Stripe.Error => BadRequest(Json.toJson(e))
      }
    })
  }
}


object CreditCardExpiryYears {
  def apply(currentYear: Int, offset: Int): List[Int] = {
    val currentYearShortened = currentYear % 100
    val subsequentYears = (currentYearShortened to currentYearShortened + offset - 2) map { _ + 1}
    currentYearShortened :: subsequentYears.toList
  }
}
