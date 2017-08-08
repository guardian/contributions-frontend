package controllers

import actions.CommonActions._
import cats.data.EitherT
import cats.instances.future._
import cats.syntax.show._
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import com.gu.i18n.{CountryGroup, Currency}
import com.netaporter.uri.Uri
import com.paypal.api.payments.Payment
import models._
import monitoring._
import play.api.mvc._
import services.{PaymentServices, PaypalService}
import play.api.data.Form
import utils.MaxAmount
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data.Forms._
import play.filters.csrf.CSRFCheck

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PaypalController(paymentServices: PaymentServices, checkToken: CSRFCheck, cloudWatchMetrics: CloudWatchMetrics)(implicit ec: ExecutionContext)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {
  import PaypalController._
  import UpdateMetaDataUtils._
  import AuthorizePaymentUtils._

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
    ophanVisitId: Option[String]
  ): Action[AnyContent] = (NoCacheAction andThen MobileSupportAction andThen ABTestAction).async { implicit request =>

    info(s"Attempting paypal payment for id: ${request.id}")
    cloudWatchMetrics.logPaymentAttempt(PaymentProvider.Paypal, request.platform)

    val paypalService = paymentServices.paypalServiceFor(request)
    val utils = new PaypalController.ExecutePaymentUtils(paypalService, countryGroup, cloudWatchMetrics)

    def storeMetaData(payment: Payment) =
      paypalService.storeMetaData(
        paymentId = paymentId,
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

    utils.executePayment(paymentId, payerId)
      .map { payment => storeMetaData(payment); payment }
      .fold(utils.notOkResult, utils.okResult)
  }

  def authorize: Action[AuthRequest] = checkToken {
    NoCacheAction.async(parse.json[AuthRequest]) { implicit request =>

      info(s"Attempting to obtain paypal auth response. Request id: ${request.id}. Platform: ${request.platform}.")
      cloudWatchMetrics.logPaymentAuthAttempt(PaymentProvider.Paypal, request.platform)

      val utils = new AuthorizePaymentUtils(cloudWatchMetrics)

      val authRequest = request.body
      val amount = utils.capAmount(authRequest.amount, authRequest.countryGroup.currency)
      val paypalService = paymentServices.paypalServiceFor(request)

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
        ophanVisitId = authRequest.ophanVisitId
      )

      payment.subflatMap(utils.authResponseFromPayment).fold(utils.notOkResult, utils.okResult)
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

  def updateMetadata(countryGroup: CountryGroup): Action[MetadataUpdate] = NoCacheAction(parse.form(metadataUpdateForm)) { implicit request =>

    val paypalService = paymentServices.paypalServiceFor(request)
    val utils = new UpdateMetaDataUtils(paypalService)

    utils.updateMarketingOptIn()
    Redirect(utils.redirectUrl(countryGroup), SEE_OTHER)
  }
}

object PaypalController {

  // Mixed in to utility classes providing methods used in Paypal Controller actions
  trait PaypalControllerUtils extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider

  // Class containing methods used to implement the authorize payment endpoint.
  // Factored out here to facilitate unit testing.
  class AuthorizePaymentUtils(cloudWatchMetrics: CloudWatchMetrics) extends PaypalControllerUtils {
    import AuthorizePaymentUtils._

    def capAmount(amount: BigDecimal, currency: Currency): BigDecimal =
      amount.min(MaxAmount.forCurrency(currency))

    def authResponseFromPayment(payment: Payment): Either[String, AuthResponse] =
      AuthResponse.fromPayment(payment)

    def notOkResult(errorMessage: String)(implicit request: Request[_]): Result = {
      error(s"Error getting PayPal auth response for request id: ${request.id}, platform: ${request.platform}.\n\t error message: $errorMessage")
      cloudWatchMetrics.logPaymentAuthFailure(PaymentProvider.Paypal, request.platform)
      InternalServerError("Error getting PayPal auth url")
    }

    def okResult(authResponse: AuthResponse)(implicit request: Request[_]): Result = {
      info(s"Paypal payment auth response successfully obtained for request id: ${request.id}, platform: ${request.platform}.")
      cloudWatchMetrics.logPaymentAuthSuccess(PaymentProvider.Paypal, request.platform)
      Ok(Json.toJson(authResponse))
    }
  }

  object AuthorizePaymentUtils {

    case class AuthRequest private (
        countryGroup: CountryGroup,
        amount: BigDecimal,
        cmp: Option[String],
        intCmp: Option[String],
        refererPageviewId: Option[String],
        refererUrl: Option[String],
        ophanPageviewId: Option[String],
        ophanBrowserId: Option[String],
        ophanVisitId: Option[String]
    )

    object AuthRequest {

      /**
        * We need to ensure there's no fragment in the URL here, as PayPal appends some query parameters to the end of it,
        * which will be removed by the browser (due to the URL stripping rules) in its requests.
        *
        * See: https://www.w3.org/TR/referrer-policy/#strip-url
        */
      def withSafeRefererUrl(
                              countryGroup: CountryGroup,
                              amount: BigDecimal,
                              cmp: Option[String],
                              intCmp: Option[String],
                              refererPageviewId: Option[String],
                              refererUrl: Option[String],
                              ophanPageviewId: Option[String],
                              ophanBrowserId: Option[String],
                              ophanVisitId: Option[String]
                            ): AuthRequest = {
        val safeRefererUrl = refererUrl.flatMap(url => Try(Uri.parse(url).copy(fragment = None).toString).toOption)
        new AuthRequest(countryGroup, amount, cmp, intCmp, refererPageviewId, safeRefererUrl, ophanPageviewId, ophanBrowserId, ophanVisitId)
      }

      private implicit val countryGroupReads = new Reads[CountryGroup] {
        override def reads(json: JsValue): JsResult[CountryGroup] = json match {
          case JsString(id) => CountryGroup.byId(id).map(JsSuccess(_)).getOrElse(JsError("invalid CountryGroup id"))
          case _ => JsError("invalid value for country group")
        }
      }

      implicit val authRequestReads: Reads[AuthRequest] = (
        (__ \ "countryGroup").read[CountryGroup] and
          (__ \ "amount").read(min[BigDecimal](1)) and
          (__ \ "cmp").readNullable[String] and
          (__ \ "intCmp").readNullable[String] and
          (__ \ "refererPageviewId").readNullable[String] and
          (__ \ "refererUrl").readNullable[String] and
          (__ \ "ophanPageviewId").readNullable[String] and
          (__ \ "ophanBrowserId").readNullable[String] and
          (__ \ "ophanVisitId").readNullable[String]
        ) (AuthRequest.withSafeRefererUrl _)
    }

    case class AuthResponse(approvalUrl: Uri, paymentId: String)

    object AuthResponse {
      import cats.syntax.either._
      import scala.collection.JavaConverters._

      def fromPayment(payment: Payment): Either[String, AuthResponse] = Either.fromOption(for {
        links <- Option(payment.getLinks)
        approvalLinks <- links.asScala.find(_.getRel.equalsIgnoreCase("approval_url"))
        approvalUrl <- Option(approvalLinks.getHref)
        paymentId <- Option(payment.getId)
      } yield AuthResponse(Uri.parse(approvalUrl), paymentId), "Unable to parse payment")

      private implicit val uriWrites = new Writes[Uri] {
        override def writes(uri: Uri): JsValue = JsString(uri.toString)
      }

      implicit val authResponseWrites: Writes[AuthResponse] = Json.writes[AuthResponse]
    }
  }

  // Class containing methods used to implement the execute payment endpoint.
  // Factored out here to facilitate unit testing.
  class ExecutePaymentUtils(paypalService: PaypalService, countryGroup: CountryGroup, cloudWatchMetrics: CloudWatchMetrics)
    extends PaypalControllerUtils {

    def executePayment(paymentId: String, payerId: String)(implicit request: ABTestRequest[AnyContent]): EitherT[Future, String, Payment] =
      paypalService.executePayment(paymentId, payerId)

    def notOkResult(errorMessage: String)(implicit request: ABTestRequest[AnyContent]): Result = {
      error(s"Error executing PayPal payment for request id: ${request.id} \n\t error message: $errorMessage")
      cloudWatchMetrics.logPaymentFailure(PaymentProvider.Paypal, request.platform)

      render {
        case Accepts.Json() => BadRequest(JsNull)
        case Accepts.Html() => Redirect(routes.Contributions.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
      }
    }

    def okResult(payment: Payment)(implicit request: ABTestRequest[AnyContent]): Result = {
      info(s"Paypal payment from platform: ${request.platform} is successful. Request id: ${request.id}.")
      cloudWatchMetrics.logPaymentSuccess(PaymentProvider.Paypal, request.platform)

      val response = render {
        case Accepts.Json() => Ok(JsNull)
        case Accepts.Html() =>
          val amount = paypalService.paymentAmount(payment)
          val email = payment.getPayer.getPayerInfo.getEmail
          val session = List("email" -> email, PaymentProvider.sessionKey -> PaymentProvider.Paypal.entryName) ++ amount.map("amount" -> _.show)
          redirectWithCampaignCodes(routes.Contributions.postPayment(countryGroup).url).addingToSession(session: _ *)
      }

      response.setCookie[ContribTimestampCookieAttributes](payment.getCreateTime)
    }
  }

  class UpdateMetaDataUtils(paypalService: PaypalService) extends PaypalControllerUtils {
    import UpdateMetaDataUtils._

    def updateMarketingOptIn()(implicit ec: ExecutionContext, request: Request[MetadataUpdate]): Future[Boolean] = {

      def onError = {
        error("email not found in session while trying to update marketing opt in")
        Future.successful(false)
      }

      def onSuccess(email: String): Future[Boolean] =
        paypalService.updateMarketingOptIn(email, request.body.marketingOptIn, IdentityId.fromRequest(request))
          .fold(_ => false, _ => true)

      request.session.data.get("email").fold(onError)(onSuccess)
    }

    def redirectUrl(countryGroup: CountryGroup)(implicit request: Request[MetadataUpdate]): String =
      request.session.get("amount").flatMap(ContributionAmount.apply)
        .filter(_ => request.isAndroid)
        .map(mobileRedirectUrl)
        .getOrElse(routes.Contributions.thanks(countryGroup).url)
  }

  object UpdateMetaDataUtils {

    case class MetadataUpdate(marketingOptIn: Boolean)

    val metadataUpdateForm: Form[MetadataUpdate] = Form(
      mapping(
        "marketingOptIn"->boolean
      )(MetadataUpdate.apply)(MetadataUpdate.unapply)
    )
  }
}
