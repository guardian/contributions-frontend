package controllers

import actions.CommonActions._
import cats.instances.future._
import cats.syntax.show._
import com.gu.i18n.{CountryGroup, Currency}
import com.netaporter.uri.Uri
import com.paypal.api.payments.Payment
import cookies.ContribTimestampCookieAttributes
import cookies.syntax._
import models._
import monitoring.{ContributionMetrics, LoggingTagsProvider}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.filters.csrf.CSRFCheck
import services.PaymentServices
import utils.MaxAmount

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PaypalController(ws: WSClient, paymentServices: PaymentServices, checkToken: CSRFCheck)(implicit ec: ExecutionContext)
  extends Controller with Redirect with LoggingTagsProvider with ContributionMetrics {

  import ContribTimestampCookieAttributes._

  val metadataUpdateForm: Form[MetadataUpdate] = Form(
    mapping(
      "marketingOptIn" -> boolean
    )(MetadataUpdate.apply)(MetadataUpdate.unapply)
  )

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
    val platform = request.platform.getOrElse("unknown")
    info(s"Attempting paypal payment for id: ${request.id} from platform: $platform")
    logPaypalPaymentAttempt()

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
        platform = request.platform,
        ophanVisitId = ophanVisitId
      )

    def notOkResult(message: String): Result = {
      error(s"Error executing PayPal payment: $message")
      logPaypalPaymentFailure(message)
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
          val amount = paypalService.paymentAmount(payment)
          val email = payment.getPayer.getPayerInfo.getEmail
          val session = List("email" -> email) ++ amount.map("amount" -> _.show)
          redirectWithCampaignCodes(routes.Contributions.postPayment(countryGroup).url).addingToSession(session: _ *)
      }
      info(s"Paypal payment successful. Payment id: ${payment.getId}")
      logPaypalPaymentSuccess()
      response.setCookie[ContribTimestampCookieAttributes](payment.getCreateTime)
    }

    paypalService.executePayment(paymentId, payerId)
      .map { payment => storeMetaData(payment); payment }
      .fold(notOkResult, okResult)
  }

  def authorize = checkToken {
    NoCacheAction.async(parse.json[AuthRequest]) { implicit request =>
      info(s"Attempting to obtain paypal auth url.")
      logPaypalAuthAttempt()
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
          error(s"Error getting PayPal auth url: $err")
          logPaypalAuthFailure()
          InternalServerError("Error getting PayPal auth url")
        },
        authResponse => {
          info(s"Paypal payment auth url successfully obtained.")
          logPaypalAuthSuccess()
          Ok(Json.toJson(authResponse))
        }
      )
    }
  }

  private def capAmount(amount: BigDecimal, currency: Currency): BigDecimal = amount min MaxAmount.forCurrency(currency)

  def hook = NoCacheAction.async(parse.tolerantText) { implicit request =>
    val bodyText = request.body
    val bodyJson = Json.parse(request.body)
    logPaypalHookAttempt()

    val paypalService = paymentServices.paypalServiceFor(request)
    val validHook = paypalService.validateEvent(request.headers.toSimpleMap, bodyText)

    def withParsedPaypalHook(paypalHookJson: JsValue)(block: PaypalHook => Future[Result]): Future[Result] = {
      bodyJson.validate[PaypalHook] match {
        case JsSuccess(paypalHook, _) if validHook =>
          info(s"Received paymentHook: ${paypalHook.paymentId}")
          logPaypalHookSuccess()
          block(paypalHook)
        case JsError(err) =>
          error(s"Unable to parse Json, parsing errors: $err")
          logPaypalHookParseError()
          Future.successful(InternalServerError("Unable to parse json payload"))
        case _ =>
          error(s"A webhook request wasn't valid: $request, headers: ${request.headers.toSimpleMap},body: $bodyText")
          logPaypalHookFailure()
          Future.successful(Forbidden("Request isn't signed by Paypal"))
      }
    }

    withParsedPaypalHook(bodyJson) { paypalHook =>
      paypalService.processPaymentHook(paypalHook).value.map {
        case Right(_) => Ok
        case Left(_) => InternalServerError
      }
    }
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

  case class AuthRequest private(
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

  case class AuthResponse(approvalUrl: Uri, paymentId: String)

  case class MetadataUpdate(marketingOptIn: Boolean)

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

}
