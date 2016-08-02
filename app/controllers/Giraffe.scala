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
import services.{AuthenticationService, PaymentServices}
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
  val maxAmount: Option[Int] = 2000.some


  def contributeRedirect = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.RestOfTheWorld)

    Redirect(routes.Giraffe.contribute(countryGroup).url, SEE_OTHER)
  }

  // Once things have settled down and we have a reasonable idea of what might
  // and might not vary between different countries, we should merge these country-specific
  // controllers & templates into a single one which varies on a number of parameters
  def contribute(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
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
    Ok(views.html.giraffe.contribute(pageInfo,maxAmount,countryGroup, chosenVariants, cmp, intCmp))
      .withCookies(Test.createCookie(chosenVariants.v1), Test.createCookie(chosenVariants.v2))
  }


  def thanks(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    val redirectUrl = routes.Giraffe.contribute(countryGroup).url

    Ok(views.html.giraffe.thankyou(PageInfo(
      title = "Thank you for supporting the Guardian",
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
      val res = stripe.Charge.create(maxAmount.fold((f.amount*100).toInt)(max => Math.min(max * 100, (f.amount * 100).toInt)), f.currency, f.email, "Your contribution", f.token, metadata)


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
