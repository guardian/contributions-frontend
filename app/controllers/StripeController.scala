package controllers

import java.lang.Math._
import java.time.Instant

import actions.CommonActions._
import cats.data.EitherT
import cats.syntax.show._
import com.gu.i18n.CountryGroup._
import com.gu.i18n.{AUD, EUR, USD}
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Charge
import com.gu.stripe.Stripe.Serializer._
import com.typesafe.config.Config
import controllers.forms.ContributionRequest
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import models._
import monitoring.LoggingTagsProvider
import org.joda.time.DateTime
import monitoring.TagAwareLogger
import play.api.libs.json._
import play.api.mvc._
import services.Ophan.OphanResponse
import services.{OphanAcquisitionEvent, OphanService, PaymentServices}
import utils.MaxAmount

import scala.concurrent.{ExecutionContext, Future}

class StripeController(paymentServices: PaymentServices, stripeConfig: Config)(implicit ec: ExecutionContext)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {

  // THIS ENDPOINT IS USED BY BOTH THE FRONTEND AND THE MOBILE-APP
  def pay = (NoCacheAction andThen MobileSupportAction andThen ABTestAction)
    .async(BodyParsers.jsonOrMultipart(ContributionRequest.contributionForm)) { implicit request =>

    val form = request.body

    val stripe = paymentServices.stripeServiceFor(request)
    val idUser = IdentityId.fromRequest(request) orElse form.idUser
    val ophanService = OphanService.ophanService

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

    def createMetaData(charge: Charge): stripe.StripeMetaData = {
      stripe.createMetaData(
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
        idUser = idUser,
        platform = form.platform orElse request.platform
      )
    }

    def storeMetaData(metadata: stripe.StripeMetaData, charge: Charge): EitherT[Future, String, SavedContributionData] = {
      stripe.storeMetaData(
        charge = charge,
        created = new DateTime(charge.created * 1000L),
        name = form.name,
        cmp = form.cmp,
        metadata = metadata.contributionMetadata,
        contributor = metadata.contributor,
        contributorRow = metadata.contributorRow,
        idUser = idUser,
        marketing = form.marketing
      )
    }

    def recordToOphan(metadata: stripe.StripeMetaData): Option[Future[OphanResponse]] = {
      val event = OphanAcquisitionEvent(metadata.contributionMetadata, metadata.contributorRow, None, PaymentProvider.Stripe)
      event.map(ophanService.submitEvent)
    }

    createCharge.map { charge =>
      val metadata = createMetaData(charge)
      storeMetaData(metadata, charge) // fire and forget. If it fails we don't want to stop the user
      recordToOphan(metadata) // again, fire and forget.
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
    NoCacheAction.async(parse.json) { implicit request =>

      def withParsedStripeHook(stripeHookJson: JsValue)(block: StripeHook => Future[Result]): Future[Result] = {
        stripeHookJson.validate[StripeHook] match {
          case JsError(err) =>
            error(s"Unable to parse the stripe hook: $err")
            Future.successful(BadRequest("Invalid Json"))
          case JsSuccess(stripeHook, _) =>
            info(s"Processing a stripe hook ${stripeHook.eventId}")
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
