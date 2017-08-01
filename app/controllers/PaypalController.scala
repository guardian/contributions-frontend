package controllers

import actions.CommonActions._
import cats.instances.future._
import cats.syntax.show._
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import com.gu.i18n.{CountryGroup, Currency}
import com.netaporter.uri.Uri
import com.paypal.api.payments.Payment
import models._
import monitoring._
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.PaymentServices
import play.api.data.Form
import utils.MaxAmount
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data.Forms._
import play.api.http.Writeable
import play.filters.csrf.CSRFCheck

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PaypalController(paymentServices: PaymentServices, checkToken: CSRFCheck, cloudWatchMetrics: CloudWatchMetrics)(implicit ec: ExecutionContext)
  extends Controller with Redirect with TagAwareLogger with LoggingTagsProvider {
  import ContribTimestampCookieAttributes._

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
  ) = (NoCacheAction andThen MobileSupportAction andThen ABTestAction).async { implicit request =>

    val paypalService = paymentServices.paypalServiceFor(request)

    info(s"Attempting paypal payment for id: ${request.sessionIdFragment}")
    cloudWatchMetrics.logPaymentAttempt(PaymentProvider.Paypal, request.platform)

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

    def notOkResult(message: String): Result = {
      error(s"Error executing PayPal payment for session id: ${request.sessionIdFragment} \n\t error message: $message")
      cloudWatchMetrics.logPaymentFailure(PaymentProvider.Paypal, request.platform)
      render {
        case Accepts.Json() => BadRequest(JsNull)
        case Accepts.Html() =>
          Redirect(routes.Contributions.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
      }
    }

    def okResult(payment: Payment): Result = {
      val response = render {
        case Accepts.Json() => Ok(JsNull)
        case Accepts.Html() =>
          val amount: Option[ContributionAmount] = paypalService.paymentAmount(payment)
          val email = payment.getPayer.getPayerInfo.getEmail
          val session = List("email" -> email, PaymentProvider.sessionKey -> PaymentProvider.Paypal.entryName) ++ amount.map("amount" -> _.show)
          redirectWithCampaignCodes(routes.Contributions.postPayment(countryGroup).url).addingToSession(session: _ *)
      }
      info(s"Paypal payment from platform: ${request.platform} is successful. Session id: ${request.sessionIdFragment}.")
      cloudWatchMetrics.logPaymentSuccess(PaymentProvider.Paypal, request.platform)
      response.setCookie[ContribTimestampCookieAttributes](payment.getCreateTime)
    }

    paypalService.executePayment(paymentId, payerId)
      .map { payment => storeMetaData(payment); payment }
      .fold(notOkResult, okResult)
  }

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
      *
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
  }

  implicit val UriWrites = new Writes[Uri] {
    override def writes(uri: Uri): JsValue = JsString(uri.toString)
  }

  implicit val AuthResponseWrites = Json.writes[AuthResponse]

  implicit val CountryGroupReads = new Reads[CountryGroup] {
    override def reads(json: JsValue): JsResult[CountryGroup] = json match {
      case JsString(id) => CountryGroup.byId(id).map(JsSuccess(_)).getOrElse(JsError("invalid CountryGroup id"))
      case _ => JsError("invalid value for country group")
    }
  }

  private def capAmount(amount: BigDecimal, currency: Currency): BigDecimal = amount min MaxAmount.forCurrency(currency)

  def authorize = checkToken {
    NoCacheAction.async(parse.json[AuthRequest]) { implicit request =>
      info(s"Attempting to obtain paypal auth response. Session id: ${request.sessionIdFragment}. Platform: ${request.platform}.")
      cloudWatchMetrics.logPaymentAuthAttempt(PaymentProvider.Paypal, request.platform)
      val authRequest = request.body
      val amount = capAmount(authRequest.amount, authRequest.countryGroup.currency)
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

      payment.subflatMap(AuthResponse.fromPayment).fold(
        err => {
          error(s"Error getting PayPal auth response for session id: ${request.sessionIdFragment}, platform: ${request.platform}.\n\t error message: $err")
          cloudWatchMetrics.logPaymentAuthFailure(PaymentProvider.Paypal, request.platform)
          InternalServerError("Error getting PayPal auth url")
        },
        authResponse => {
          info(s"Paypal payment auth response successfully obtained for session id: ${request.sessionIdFragment}, platform: ${request.platform}.")
          cloudWatchMetrics.logPaymentAuthSuccess(PaymentProvider.Paypal, request.platform)
          Ok(Json.toJson(authResponse))
        }
      )
    }
  }

  def hook = NoCacheAction.async(parse.tolerantText) { implicit request =>
    val bodyText = request.body
    val bodyJson = Json.parse(request.body)

    info(s"Paypal hook attempt made for session id: ${request.sessionIdFragment}, Platform: ${request.platform}")
    cloudWatchMetrics.logHookAttempt(PaymentProvider.Paypal, request.platform)

    val paypalService = paymentServices.paypalServiceFor(request)
    val validHook = paypalService.validateEvent(request.headers.toSimpleMap, bodyText)

    def withParsedPaypalHook(paypalHookJson: JsValue)(block: PaypalHook => Future[Result]): Future[Result] = {
      bodyJson.validate[PaypalHook] match {
        case JsSuccess(paypalHook, _) if validHook =>
          info(s"Received and parsed paymentHook: ${paypalHook.paymentId} for session id: ${request.sessionIdFragment}, platform: ${request.platform}.")
          cloudWatchMetrics.logHookParsed(PaymentProvider.Paypal, request.platform)
          block(paypalHook)
        case JsError(err) =>
          error(s"Unable to parse Json for session id: ${request.sessionIdFragment}, platform: ${request.sessionIdFragment}.\n\t parsing errors: $err")
          cloudWatchMetrics.logHookParseError(PaymentProvider.Paypal, request.platform)
          Future.successful(InternalServerError("Unable to parse json payload"))
        case _ =>
          error(s"A paypal webhook request wasn't valid. Session id: ${request.sessionIdFragment}. Platform: ${request.platform}.\n\tRequest is: $request, headers: ${request.headers.toSimpleMap},body: $bodyText")
          cloudWatchMetrics.logHookInvalidRequest(PaymentProvider.Paypal, request.platform)
          Future.successful(Forbidden("Request isn't signed by Paypal"))
      }
    }

    withParsedPaypalHook(bodyJson) { paypalHook =>
      paypalService.processPaymentHook(paypalHook).value.map {
        case Right(_) => {
          info(s"Paypal hook: ${paypalHook.paymentId} processed successfully for session id: ${request.sessionIdFragment}, platform: ${request.platform}.")
          cloudWatchMetrics.logHookProcessed(PaymentProvider.Paypal, request.platform)
          Ok
        }
        case Left(err) => {
          error(s"Paypal hook: ${paypalHook.paymentId} processing error. Session id: ${request.sessionIdFragment}, platform: ${request.platform}. \n\t error: $err")
          cloudWatchMetrics.logHookProcessError(PaymentProvider.Paypal, request.platform)
          InternalServerError
        }
      }
    }
  }
  case class MetadataUpdate(marketingOptIn: Boolean)

  val metadataUpdateForm: Form[MetadataUpdate] = Form(
    mapping(
      "marketingOptIn"->boolean
    )(MetadataUpdate.apply)(MetadataUpdate.unapply)
  )

  def updateMetadata(countryGroup: CountryGroup) = NoCacheAction.async(parse.form(metadataUpdateForm)) { implicit request =>
    val paypalService = paymentServices.paypalServiceFor(request)
    val marketingOptIn = request.body.marketingOptIn
    val idUser = IdentityId.fromRequest(request)
    val contributor = request.session.data.get("email") match {
      case Some(email) => paypalService.updateMarketingOptIn(email, marketingOptIn, idUser).value
      case None => Future.successful(error("email not found in session while trying to update marketing opt in"))
    }

    val url = request.session.get("amount").flatMap(ContributionAmount.apply)
      .filter(_ => request.isAndroid)
      .map(mobileRedirectUrl)
      .getOrElse(routes.Contributions.thanks(countryGroup).url)

    contributor.map { _ =>
      Redirect(url, SEE_OTHER)
    }
  }
}
