package controllers

import java.lang.Math.min
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

import actions.CommonActions.NoCacheAction
import com.gu.i18n.CountryGroup._
import com.gu.i18n._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.netaporter.uri.dsl._
import configuration.Config
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.mvc._
import services.PaymentServices
import utils.RequestCountry._
import views.support._
import scala.concurrent.Future

class Giraffe(paymentServices: PaymentServices) extends Controller {
  val abTestFormatter: Formatter[JsValue] = new Formatter[JsValue] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError],JsValue] = {
      val parse: JsValue = Json.parse(URLDecoder.decode(data(key),StandardCharsets.UTF_8.name()))
      Right(parse)
    }
    override def unbind(key: String, data: JsValue): Map[String,String] = Map()

  }
  implicit val currencyFormatter = new Formatter[Currency] {
    type Result = Either[Seq[FormError], Currency]
    override def bind(key: String, data: Map[String, String]): Result =
      data.get(key).map(_.toUpperCase).flatMap(Currency.fromString).fold[Result](Left(Seq.empty))(currency => Right(currency))
    override def unbind(key: String, value: Currency): Map[String, String] =
      Map(key -> value.identifier)
  }

  case class SupportForm(
                          name: String,
                          currency: Currency,
                          amount: BigDecimal,
                          email: String,
                          token: String,
                          marketing: Boolean,
                          postCode: Option[String],
                          abTests: JsValue,
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
      "payment.token" -> nonEmptyText,
      "guardian-opt-in" -> boolean,
      "postcode" -> optional(nonEmptyText),
      "abTest" -> FieldMapping[JsValue]()(abTestFormatter),
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

  def contributeRedirect = NoCacheAction { implicit request =>

    val countryGroup = request.getFastlyCountry match {
      case Some(Canada) | Some(NewZealand) | Some(RestOfTheWorld) => UK
      case Some(other) => other
      case None => UK
    }

    redirectWithCampaignCodes(routes.Giraffe.contribute(countryGroup).url)
  }

  def redirectToUk = NoCacheAction { implicit request => redirectWithCampaignCodes(routes.Giraffe.contribute(UK).url) }

  private def redirectWithCampaignCodes(destinationUrl: String)(implicit request: Request[Any]) = {
    val CampaignCodesToForward = Set("INTCMP", "CMP", "mcopy")
    Redirect(destinationUrl, request.queryString.filterKeys(CampaignCodesToForward), SEE_OTHER)
  }

  private def getErrorMessage(errorCode: Option[String]) = errorCode match {
    case Some("paypalError") => Some("Your contribution using paypal could not be processed. Please try again or use another payment method.")
    case _ => None
  }

  def contribute(countryGroup: CountryGroup, errorCode: Option[String] = None) = NoCacheAction { implicit request =>
    val errorMessage = getErrorMessage(errorCode)
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
    val maxAmountInLocalCurrency = configuration.Payment.maxAmountFor(countryGroup.currency)
    val creditCardExpiryYears = CreditCardExpiryYears(LocalDate.now.getYear, 10)
    Ok(views.html.giraffe.contribute(pageInfo,maxAmountInLocalCurrency,countryGroup, chosenVariants, cmp, intCmp, creditCardExpiryYears, errorMessage))
      .withCookies(Test.createCookie(chosenVariants.v1), Test.createCookie(chosenVariants.v2), Test.createCookie(chosenVariants.paymentMethodTest))

  }

  def thanks(countryGroup: CountryGroup) = NoCacheAction { implicit request =>

    val title = countryGroup match {
      case Australia => "Thank you for supporting Guardian Australia"
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
    },{ f =>
      val metadata = Map(
        "marketing-opt-in" -> f.marketing.toString,
        "email" -> f.email,
        "name" -> f.name,
        "abTests" -> f.abTests.toString,
        "ophanId" -> f.ophanId,
        "cmp" -> f.cmp.mkString,
        "intcmp" -> f.intcmp.mkString
      )  ++ f.postCode.map("postcode" -> _)

      // Note that '.. * 100' will not work for Yen and other currencies! https://stripe.com/docs/api#charge_object-amount
      val amountInSmallestCurrencyUnit = (f.amount * 100).toInt
      val maxAmountInSmallestCurrencyUnit = configuration.Payment.maxAmountFor(f.currency) * 100
      val res = stripe.Charge.create(min(maxAmountInSmallestCurrencyUnit, amountInSmallestCurrencyUnit), f.currency, f.email, "Your contribution", f.token, metadata)


      val redirect = f.currency match {
        case USD => routes.Giraffe.thanks(US).url
        case AUD => routes.Giraffe.thanks(Australia).url
        case EUR => routes.Giraffe.thanks(Europe).url
        case _ => routes.Giraffe.thanks(UK).url
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
