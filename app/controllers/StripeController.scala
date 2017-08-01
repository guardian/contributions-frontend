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
import com.gu.zuora.soap.models.Queries.PaymentMethod
import com.typesafe.config.Config
import controllers.forms.ContributionRequest
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import models._
import monitoring.{CloudWatchMetrics, LoggingTagsProvider, TagAwareLogger}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc._
import services.Ophan.OphanResponse
import services.{OphanAcquisitionEvent, OphanService, PaymentServices}
import utils.MaxAmount

import scala.concurrent.{ExecutionContext, Future}

class StripeController(paymentServices: PaymentServices, stripeConfig: Config, ophanService: OphanService, cloudWatchMetrics: CloudWatchMetrics)(implicit ec: ExecutionContext)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {

  def payOptions = CachedAction { request =>
    NoContent.withHeaders(("Vary" -> "Origin") :: corsHeaders(request): _*)
  }

  // THIS ENDPOINT IS USED BY BOTH THE FRONTEND AND THE MOBILE-APP
  def pay = (NoCacheAction andThen MobileSupportAction andThen ABTestAction)
    .async(BodyParsers.jsonOrMultipart(ContributionRequest.contributionForm)) { implicit request =>
    info(s"A Stripe payment is being attempted with session id: ${request.sessionIdFragment}, from platform: ${request.platform}.")
    cloudWatchMetrics.logPaymentAttempt(PaymentProvider.Stripe, request.platform)

    val form = request.body

    val stripe = paymentServices.stripeServiceFor(form.name)
    val idUser = IdentityId.fromRequest(request) orElse form.idUser

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
      "ophanVisitId" -> form.ophanVisitId.getOrElse(""),
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
      info(s"Payment successful for request ${request.sessionIdFragment} - redirected to external platform for thank you page. platform is: ${request.platform}.")
      cloudWatchMetrics.logPaymentSuccessRedirected(PaymentProvider.Stripe, request.platform)
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
        platform = Some(request.platform),
        ophanVisitId = form.ophanVisitId
      )
    }

    def storeMetaData(metadata: stripe.StripeMetaData): EitherT[Future, String, SavedContributionData] = {
      stripe.storeMetaData(
        created = metadata.contributionMetadata.created,
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
      info(s"Stripe payment successful for session id: ${request.sessionIdFragment}, from platform ${request.platform}")
      cloudWatchMetrics.logPaymentSuccess(PaymentProvider.Stripe, request.platform)
      val metadata = createMetaData(charge)
      storeMetaData(metadata) // fire and forget. If it fails we don't want to stop the user
      recordToOphan(metadata) // again, fire and forget.
      Ok(Json.obj("redirect" -> thankYouUri))
        .addingToSession("charge_id" -> charge.id)
        .addingToSession("amount" -> contributionAmount.show)
        .addingToSession(PaymentProvider.sessionKey -> PaymentProvider.Stripe.entryName)
        .setCookie[ContribTimestampCookieAttributes](Instant.ofEpochSecond(charge.created).toString)
        .withHeaders(corsHeaders(request): _*)
    }.recover {
      case e: Stripe.Error => {
        warn(s"Payment failed for session id: ${request.sessionIdFragment}, from platform: ${request.platform}, \n\t with code: ${e.decline_code} \n\t and message: ${e.message}.")
        cloudWatchMetrics.logPaymentFailure(PaymentProvider.Stripe, request.platform)
        BadRequest(Json.toJson(e)).withHeaders(corsHeaders(request): _*)
      }
    }
  }

  private val allowedOrigins = stripeConfig.getStringList("cors.allowedOrigins")

  private def corsHeaders(request: Request[_]) = {
    val origin = request.headers.get("origin")
    val allowedOrigin = origin.filter(allowedOrigins.contains)
    allowedOrigin.toList.flatMap { origin =>
      List(
        "Access-Control-Allow-Origin" -> origin,
        "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept"
      )
    }
  }

  val webhookKey = stripeConfig.getString("webhook.key")

  def hook = SharedSecretAction(webhookKey) {
    NoCacheAction.async(parse.json) { implicit request =>

      cloudWatchMetrics.logHookAttempt(PaymentProvider.Stripe, request.platform)

      def withParsedStripeHook(stripeHookJson: JsValue)(block: StripeHook => Future[Result]): Future[Result] = {
        stripeHookJson.validate[StripeHook] match {
          case JsError(err) =>
            error(s"Unable to parse the stripe hook for session id: ${request.sessionIdFragment}, from platform: ${request.platform}. \n\tFailed with message: $err")
            cloudWatchMetrics.logHookParseError(PaymentProvider.Stripe, request.platform)
            Future.successful(BadRequest("Invalid Json"))
          case JsSuccess(stripeHook, _) =>
            info(s"Processing a stripe hook for session id: ${request.sessionIdFragment}, from platform: ${request.platform}. Stripe Hook id is: ${stripeHook.eventId}")
            cloudWatchMetrics.logHookParsed(PaymentProvider.Stripe, request.platform)
            block(stripeHook)
        }
      }

      withParsedStripeHook(request.body) { stripeHook =>
        val stripeService = paymentServices.stripeServices(stripeHook.mode)
        stripeService.processPaymentHook(stripeHook)
          .value.map {
          case Right(_) => {
            info(s"Stripe hook: ${stripeHook.paymentId} processed successfully for session id: ${request.sessionIdFragment}, from platform ${request.platform}.")
            cloudWatchMetrics.logHookProcessed(PaymentProvider.Stripe, request.platform)
            Ok
          }
          case Left(err) => {
            error(s"Stripe hook: ${stripeHook.paymentId} processing error for session id: ${request.sessionIdFragment}, from platform: ${request.platform} \n\t error: $err")
            cloudWatchMetrics.logHookProcessError(PaymentProvider.Stripe, request.platform)
            InternalServerError
          }
        }
      }
    }
  }
}
