package controllers

import java.lang.Math._
import java.time.Instant

import actions.CommonActions._
import cats.data.EitherT
import cats.syntax.show._
import com.gu.i18n.CountryGroup._
import com.gu.i18n.{AUD, Currency, EUR, USD}
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Charge
import com.gu.stripe.Stripe.Serializer._
import com.typesafe.config.Config
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{BodyParsers, Controller, Result}
import services.PaymentServices
import utils.MaxAmount

import scala.concurrent.{ExecutionContext, Future}

class StripeController(paymentServices: PaymentServices, stripeConfig: Config)(implicit ec: ExecutionContext)
  extends Controller with Redirect {

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
    val contributionAmount = ContributionAmount(BigDecimal(amount, 2), form.currency)

    def thankYouUri = if (request.isAndroid) {
      mobileRedirectUrl(contributionAmount)
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
        .addingToSession("charge_id" -> charge.id)
        .addingToSession("amount" -> contributionAmount.show)
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

  case class AppContributionRequest(
    name: String,
    email: String,
    currency: Currency,
    amount: BigDecimal,
    token: String,
    platform: String,
    ophanPageViewId: String,
    intcmp: Option[String],
    ophanBrowserId: Option[String],
    referrerPageViewId: Option[String],
    referrerUrl: Option[String]
  )

  object AppContributionRequest {
    implicit val currencyFormatter: Reads[Currency] = new Reads[Currency] {
      override def reads(json: JsValue): JsResult[Currency] = json match {
        case JsString(symbol) => Currency.fromString(symbol).map(currency => JsSuccess.apply(currency)).getOrElse(JsError(s"Unable to parse $symbol"))
        case _ => JsError("Unable to parse currency, was expecting a JsString")
      }
    }
    implicit val requestReads: Reads[AppContributionRequest] = (
      (JsPath \ "name").read[String] and
        (JsPath \ "email").read[String](Reads.email) and
        (JsPath \ "currency").read[Currency] and
        (JsPath \ "amount").read[BigDecimal] and
        (JsPath \ "token").read[String] and
        (JsPath \ "platform").read[String] and
        (JsPath \ "ophanPageViewId").read[String] and
        (JsPath \ "intcmp").readNullable[String] and
        (JsPath \ "ophanBrowserId").readNullable[String] and
        (JsPath \ "referrerPageViewId").readNullable[String] and
        (JsPath \ "referrerUrl").readNullable[String]
    )(AppContributionRequest.apply _)
  }


  def appPay = NoCacheAction.async(BodyParsers.parse.json[AppContributionRequest]) { request =>
    val stripe = paymentServices.stripeServiceFor(request)

    val contributionId = ContributionId.random
    val userId = IdentityId.fromRequest(request) // TODO (won't work)

    // Get the contribution amount in pence/cents/similar (only works for some currencies)
    val amountInSmallestCurrencyUnit = (request.body.amount * 100).toInt

    // Cap the contribution amount, according to currency
    // TODO: I think that if the amount is too big then that's an error and we should report back to the user and ask
    // them what they want to do rather than charging them an amount they didn't specify, but I'll leave this as it is
    // at the moment because this is the behaviour of the existing pay method
    val maxAmountInSmallestCurrencyUnit = MaxAmount.forCurrency(request.body.currency) * 100
    val amount = min(maxAmountInSmallestCurrencyUnit, amountInSmallestCurrencyUnit)

    // Pull together the metadata for this contribution into a map
    val metadata = Map(
      "email" -> request.body.email,
      "name" -> request.body.name,
      "platform" -> request.body.platform,
      "contributionId" -> contributionId.toString,
      "ophanPageviewId" -> request.body.ophanPageViewId
    ) ++ List(
      request.body.intcmp.map("intcmp" -> _),
      request.body.ophanBrowserId.map("ophanBrowserId" -> _),
      request.body.referrerPageViewId.map("refererPageviewId" -> _),
      request.body.referrerUrl.map("refererUrl" -> _),
      userId.map("idUser" -> _.id)
    ).flatten.toMap

    def createCharge: Future[Stripe.Charge] = {
      stripe.Charge.create(amount, request.body.currency, request.body.email, "Your contribution", request.body.token, metadata)
    }

    def storeMetaData(charge: Charge) = {
      stripe.storeMetaData(
        contributionId = contributionId,
        charge = charge,
        created = new DateTime(charge.created * 1000L),
        name = request.body.name,
        postCode = None,
        marketing = false,
        testAllocations = Set.empty,
        cmp = None,
        intCmp = request.body.intcmp,
        refererPageviewId = request.body.referrerPageViewId,
        refererUrl = request.body.referrerUrl,
        ophanPageviewId = request.body.ophanPageViewId,
        ophanBrowserId = request.body.ophanBrowserId,
        idUser = userId
      )
    }

    createCharge.map { charge =>
      storeMetaData(charge)
      Ok
    }.recover {
      case e: Stripe.Error => BadRequest(Json.toJson(e))
    }
  }

}
