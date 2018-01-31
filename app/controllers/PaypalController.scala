package controllers

import actions.CommonActions._
import cats.instances.future._
import cats.syntax.show._
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import com.gu.i18n.{CountryGroup, Currency}
import com.paypal.api.payments.Payment
import configuration.CorsConfig
import controllers.httpmodels.{AuthRequest, AuthResponse, CaptureRequest}
import models._
import monitoring._
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.mvc._
import services.{ContributionOphanService, PaymentServices, PaypalService}
import utils.MaxAmount
import play.api.libs.json._
import play.filters.csrf.CSRFCheck

import scala.concurrent.{ExecutionContext, Future}

class PaypalController(paymentServices: PaymentServices, corsConfig: CorsConfig,
  checkToken: CSRFCheck, cloudWatchMetrics: CloudWatchMetrics, ophanService: ContributionOphanService)(implicit ec: ExecutionContext)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {
  import ContribTimestampCookieAttributes._

  def authorizeOptions = CachedAction { request =>
    NoContent.withHeaders(("Vary" -> "Origin") :: corsHeaders(request): _*)
  }

  private def corsHeaders(request: Request[_]) = {
    val origin = request.headers.get("origin")
    val allowedOrigin = origin.filter(corsConfig.allowedOrigins.contains)
    allowedOrigin.toList.flatMap { origin =>
      List(
        "Access-Control-Allow-Origin" -> origin,
        "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept",
        "Access-Control-Allow-Credentials" -> "true"
      )
    }
  }

  def capturePayment = NoCacheAction.andThen(MetaDataAction.default).async(parse.json[CaptureRequest]) { implicit request =>
    val captureBody = request.body
    val paypalService: PaypalService = paymentServices.paypalServiceFor(request)

    def storeMetaData(payment: Payment) =
      paypalService.storeMetaData(
        payment = payment,
        testAllocations = request.testAllocations,
        cmp = captureBody.cmp,
        intCmp = captureBody.intCmp,
        refererPageviewId = captureBody.refererPageviewId,
        refererUrl = captureBody.refererUrl,
        ophanPageviewId = captureBody.ophanPageviewId,
        ophanBrowserId = captureBody.ophanBrowserId,
        idUser = captureBody.idUser,
        platform = Some(captureBody.platform),
        ophanVisitId = None
      )
      .leftMap { err =>
        error(
          "Unable to store the metadata while capturing the payment. Continuing anyway." +
          s"Contributions session id: ${request.sessionId} Error: $err"
        )
      }

    paypalService.capturePayment(captureBody.paymentId)
      .map { capture =>
        // Executed for side-effects only
        paypalService.getPayment(capture.getParentPayment)
          .fold(
            err => error(
              s"Unable to retrieve payment from capture due to a Paypal API error: $err " +
              s"Payment not stored, nor sent to Ophan. Contributions session id: ${request.sessionId}"
            ),
            payment => {
              storeMetaData(payment)
              ophanService.submitAcquisition(PaypalAcquisitionComponents.Capture(payment, request))
            }
          )

        capture
      }
      .fold(
        err => {
          error(s"Unable to capture the payment for contributions session id: ${request.sessionId}. Error message is: $err")
          InternalServerError(Json.toJson(err))
        },
        _ => Ok
      )
  }

  def executePayment(
    countryGroup: CountryGroup,
    paymentId: String,
    token: String,
    payerId: String,
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String],
    ophanVisitId: Option[String],
    componentId: Option[String],
    componentType: Option[ComponentType],
    source: Option[AcquisitionSource],
    refererAbTest: Option[AbTest],
    nativeAbTests: Option[Set[AbTest]],
    supportRedirect: Option[Boolean]
  ) = NoCacheAction.andThen(MobileSupportAction).andThen(MetaDataAction.default).async { implicit request =>
    val paypalService = paymentServices.paypalServiceFor(request)

    info(s"Attempting paypal payment for contributions session id: ${request.sessionId}")
    cloudWatchMetrics.logPaymentAttempt(PaymentProvider.Paypal, request.platform)

    def storeMetaData(payment: Payment) =
      paypalService.storeMetaData(
        payment = payment,
        testAllocations = request.testAllocations,
        cmp = cmp,
        intCmp = intCmp,
        refererPageviewId = refererPageviewId,
        refererUrl = refererUrl,
        ophanPageviewId = ophanPageviewId,
        ophanBrowserId = ophanBrowserId,
        idUser = IdentityId.fromRequest(request),
        platform = Some(request.platform),
        ophanVisitId = ophanVisitId
      )

    def notOkResult(paypalError: PaypalApiError): Result = {

      val message = s"Error executing PayPal payment for contributions session id: ${request.sessionId} " +
        s"error message: ${paypalError.message}"

      paypalError.errorType match {
        // This signifies an issue with the user's account. Don't pollute error level logs with such issues.
        case PaypalErrorType.InstrumentDeclined => warn(message)
        case _ => error(message)
      }

      cloudWatchMetrics.logPaymentFailure(PaymentProvider.Paypal, request.platform)

      render {
        case Accepts.Json() => BadRequest(JsNull)
        case Accepts.Html() =>
          Redirect(routes.Contributions.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
      }
    }

    def okResult(payment: Payment): Result = {
      val response = render {
        case Accepts.Json() if supportRedirect.contains(true) => Ok(Json.obj("email" -> payment.getPayer.getPayerInfo.getEmail))
        case Accepts.Json() => Ok(JsNull)
        case Accepts.Html() =>
          val amount: Option[ContributionAmount] = paypalService.paymentAmount(payment)
          val email = payment.getPayer.getPayerInfo.getEmail
          val session = List("email" -> email, PaymentProvider.sessionKey -> PaymentProvider.Paypal.entryName) ++ amount.map("amount" -> _.show)

          val redirectUrl = routes.Contributions.postPayment(countryGroup).url

          info(s"Paypal payment from platform: ${request.platform} is successful. Contributions session id: ${request.sessionId}. Amount is ${amount.map(_.show).getOrElse("")}.")
          redirect(redirectUrl).addingToSession(session: _ *)
      }
      cloudWatchMetrics.logPaymentSuccess(PaymentProvider.Paypal, request.platform)
      if (supportRedirect.contains(true)) {
        info(s"Redirecting user to support thank-you page. Payment method used: Paypal, platform: ${request.platform} , contributions session id: ${request.sessionId}.")
        cloudWatchMetrics.logPaymentSuccessRedirected(PaymentProvider.Paypal, request.platform)
      }
      response.setCookie[ContribTimestampCookieAttributes](payment.getCreateTime)
    }

    def requestData =
      PaypalAcquisitionComponents.Execute.RequestData(
        cmp = cmp,
        intCmp = intCmp,
        refererPageviewId = refererPageviewId,
        refererUrl = refererUrl,
        ophanPageviewId = ophanPageviewId,
        ophanBrowserId = ophanBrowserId,
        ophanVisitId = ophanVisitId,
        componentId = componentId,
        componentType = componentType,
        source = source,
        refererAbTest = refererAbTest,
        nativeAbTests = nativeAbTests,
        isSupport = supportRedirect
      )

    paypalService.executePayment(paymentId, payerId)
      .map { payment =>
        storeMetaData(payment)
        ophanService.submitAcquisition(PaypalAcquisitionComponents.Execute(payment, requestData))
        payment
      }
      .fold(notOkResult, okResult)
  }

  private def capAmount(amount: BigDecimal, currency: Currency): BigDecimal = amount min MaxAmount.forCurrency(currency)

  def authorize = checkToken {
    NoCacheAction.andThen(MetaDataAction.default).async(parse.json[AuthRequest]) { implicit request =>
      info(s"Attempting to obtain paypal auth response. Contributions session id: ${request.sessionId}. Platform: ${request.platform}.")
      cloudWatchMetrics.logPaymentAuthAttempt(PaymentProvider.Paypal, request.platform)
      val authRequest = request.body
      val amount = capAmount(authRequest.amount, authRequest.countryGroup.currency)
      val paypalService = paymentServices.paypalServiceFor(request)
      val supportRedirect = authRequest.supportRedirect
      val payment = paypalService.getPayment(
        amount = amount,
        countryGroup = authRequest.countryGroup,
        contributionId = ContributionId.random,
        cmp = authRequest.cmp,
        intCmp = authRequest.intCmp,
        refererPageviewId = authRequest.refererPageviewId,
        refererUrl = authRequest.refererUrl,
        ophanPageviewId = authRequest.ophanPageviewId,
        ophanBrowserId = authRequest.ophanBrowserId,
        ophanVisitId = authRequest.ophanVisitId,
        componentId = authRequest.componentId,
        componentType = authRequest.componentType,
        source = authRequest.source,
        refererAbTest = authRequest.refererAbTest,
        nativeAbTests = authRequest.nativeAbTests,
        supportRedirect = authRequest.supportRedirect
      )

      payment.subflatMap(AuthResponse.fromPayment).fold(
        err => {
          error(s"Error getting PayPal auth response for contributions session id: ${request.sessionId}, platform: ${request.platform}.\n\t error message: $err")
          cloudWatchMetrics.logPaymentAuthFailure(PaymentProvider.Paypal, request.platform)
          InternalServerError("Error getting PayPal auth url").withHeaders(corsHeaders(request): _*)
        },
        authResponse => {
          info(s"Paypal payment auth response successfully obtained for contributions session id: ${request.sessionId}, platform: ${request.platform}.")
          cloudWatchMetrics.logPaymentAuthSuccess(PaymentProvider.Paypal, request.platform)
          Ok(Json.toJson(authResponse)).withHeaders(corsHeaders(request): _*)
        }
      )
    }
  }

  def hook = NoCacheAction.async(parse.tolerantText) { implicit request =>
    val bodyText = request.body
    val bodyJson = Json.parse(request.body)

    info(s"Paypal hook attempt made for request id: ${request.id}, Platform: ${request.platform}")
    cloudWatchMetrics.logHookAttempt(PaymentProvider.Paypal, request.platform)

    val paypalService = paymentServices.paypalServiceFor(request)
    val validHook = paypalService.validateEvent(request.headers.toSimpleMap, bodyText)

    def withParsedPaypalHook(paypalHookJson: JsValue)(block: PaypalHook => Future[Result]): Future[Result] = {
      bodyJson.validate[PaypalHook] match {
        case JsSuccess(paypalHook, _) if validHook =>
          info(s"Received and parsed paymentHook: ${paypalHook.paymentId} for request id: ${request.id}, platform: ${request.platform}.")
          cloudWatchMetrics.logHookParsed(PaymentProvider.Paypal, request.platform)
          block(paypalHook)
        case JsError(err) =>
          error(s"Unable to parse Json for request id: ${request.id}, platform: ${request.platform}.\n\t parsing errors: $err")
          cloudWatchMetrics.logHookParseError(PaymentProvider.Paypal, request.platform)
          Future.successful(InternalServerError("Unable to parse json payload"))
        case _ =>
          error(s"A paypal webhook request wasn't valid. Request id: ${request.id}. Platform: ${request.platform}.\n\tRequest is: $request, headers: ${request.headers.toSimpleMap},body: $bodyText")
          cloudWatchMetrics.logHookInvalidRequest(PaymentProvider.Paypal, request.platform)
          Future.successful(Forbidden("Request isn't signed by Paypal"))
      }
    }

    withParsedPaypalHook(bodyJson) { paypalHook =>
      paypalService.processPaymentHook(paypalHook).value.map {
        case Right(_) => {
          info(s"Paypal hook: ${paypalHook.paymentId} processed successfully for request id: ${request.id}, platform: ${request.platform}.")
          cloudWatchMetrics.logHookProcessed(PaymentProvider.Paypal, request.platform)
          Ok
        }
        case Left(err) => {
          error(s"Paypal hook: ${paypalHook.paymentId} processing error. Request id: ${request.id}, platform: ${request.platform}. \n\t error: $err")
          cloudWatchMetrics.logHookProcessError(PaymentProvider.Paypal, request.platform)
          InternalServerError
        }
      }
    }
  }
}
