package controllers

import java.lang.Math._
import java.time.Instant

import actions.CommonActions._
import cats.data.EitherT
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import com.gu.i18n.CountryGroup._
import com.gu.i18n.{AUD, Currency, EUR, USD}
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Charge
import com.gu.stripe.Stripe.Serializer._
import com.typesafe.config.Config
import cookies.ContribTimestampCookieAttributes
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{BodyParsers, Controller, Result}
import services.PaymentServices
import utils.MaxAmount

import scala.concurrent.{ExecutionContext, Future}

class StripeController(paymentServices: PaymentServices, stripeConfig: Config)(implicit ec: ExecutionContext)
  extends Controller with Redirect {
  import ContribTimestampCookieAttributes._

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
    intcmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String]

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
      "intcmp" -> optional(text),
      "refererPageviewId" -> optional(text),
      "refererUrl" -> optional(text)
    )(SupportForm.apply)(SupportForm.unapply)
  )

  def pay = (NoCacheAction andThen MobileSupportAction andThen ABTestAction).async(parse.form(supportForm)) { implicit request =>

    val form = request.body

    val stripe = paymentServices.stripeServiceFor(request)
    val idUser = IdentityId.fromRequest(request)

    val countryGroup = form.currency match {
      case USD => US
      case AUD => Australia
      case EUR => Europe
      case _ => UK
    }

    val contributionId = ContributionId.random

    val metadata = Map(
      "marketing-opt-in" -> form.marketing.toString,
      "email" -> form.email,
      "name" -> form.name,
      "abTests" -> Json.toJson(request.testAllocations).toString,
      "ophanPageviewId" -> form.ophanPageviewId,
      "ophanBrowserId" -> form.ophanBrowserId.getOrElse(""),
      "cmp" -> form.cmp.mkString,
      "intcmp" -> form.intcmp.mkString,
      "refererPageviewId" -> form.refererPageviewId.mkString,
      "refererUrl" -> form.refererUrl.mkString,
      "contributionId" -> contributionId.toString
    ) ++ List(
      form.postcode.map("postcode" -> _),
      idUser.map("idUser" -> _.id)
    ).flatten.toMap
    // Note that '.. * 100' will not work for Yen and other currencies! https://stripe.com/docs/api#charge_object-amount
    val amountInSmallestCurrencyUnit = (form.amount * 100).toInt
    val maxAmountInSmallestCurrencyUnit = MaxAmount.forCurrency(form.currency) * 100
    val amount = min(maxAmountInSmallestCurrencyUnit, amountInSmallestCurrencyUnit)

    def thankYouUri = if (request.isMobile) {
      thankYouMobileUri(ContributionAmount(BigDecimal(amount, 2), form.currency))
    } else {
      routes.Contributions.thanks(countryGroup).url
    }

    def createCharge: Future[Stripe.Charge] = {
      stripe.Charge.create(amount, form.currency, form.email, "Your contribution", form.token, metadata)
    }

    def storeMetaData(charge: Charge): EitherT[Future, String, SavedContributionData] = {
      stripe.storeMetaData(
        contributionId = contributionId,
        charge = charge,
        created = new DateTime(charge.created * 1000L),
        name = form.name,
        postCode = form.postcode,
        marketing = form.marketing,
        testAllocations = request.testAllocations,
        cmp = form.cmp,
        intCmp = form.intcmp,
        refererPageviewId = form.refererPageviewId,
        refererUrl = form.refererUrl,
        ophanPageviewId = form.ophanPageviewId,
        ophanBrowserId = form.ophanBrowserId,
        idUser = idUser
      )
    }

    createCharge.map { charge =>
      storeMetaData(charge) // fire and forget. If it fails we don't want to stop the user
      Ok(Json.obj("redirect" -> thankYouUri))
        .withSession("charge_id" -> charge.id)
        .setCookie[ContribTimestampCookieAttributes](Instant.ofEpochSecond(charge.created).toString)
    }.recover {
      case e: Stripe.Error => BadRequest(Json.toJson(e))
    }
  }

  val webhookKey = stripeConfig.getString("webhook.key")

  def hook = SharedSecretAction(webhookKey) {
    NoCacheAction.async(BodyParsers.parse.json) { request =>

      def withParsedStripeHook(stripeHookJson: JsValue)(block: StripeHook => Future[Result]): Future[Result] = {
        stripeHookJson.validate[StripeHook] match {
          case JsError(error) =>
            Logger.error(s"Unable to parse the stripe hook: $error")
            Future.successful(BadRequest("Invalid Json"))
          case JsSuccess(stripeHook, _) =>
            Logger.info(s"Processing a stripe hook ${stripeHook.eventId}")
            block(stripeHook)
        }
      }

      withParsedStripeHook(request.body) { stripeHook =>
        val stripeService = paymentServices.stripeServices(stripeHook.mode)
        stripeService.processPaymentHook(stripeHook)
          .value.map {
          case Right(_) => Ok
          case Left(_) => InternalServerError
        }
      }
    }
  }
}
