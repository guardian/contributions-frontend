package controllers

import java.util.UUID

import actions.CommonActions._
import com.gu.i18n.{CountryGroup, Currency}
import com.paypal.api.payments.Payment
import models.PaymentHook
import org.joda.time.DateTime
import play.api.libs.ws.WSClient
import play.api.mvc.{BodyParsers, Controller, Result}
import services.PaymentServices
import play.api.Logger
import play.api.data.Form
import utils.ContributionIdGenerator
import views.support.Test
import utils.MaxAmount

import scala.util.{Failure, Right, Success, Try}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.Future

class PaypalController(
  ws: WSClient,
  paymentServices: PaymentServices
) extends Controller {

  def executePayment(
    countryGroup: CountryGroup,
    paymentId: String,
    token: String,
    payerId: String,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  ) = NoCacheAction { implicit request =>
    val chosenVariants = Test.getContributePageVariants(countryGroup, request)
    val paypalService = paymentServices.paypalServiceFor(request)
    val queryParams = Map("CMP" -> cmp.toSeq, "INTCMP" -> intCmp.toSeq)
    val idUser = IdentityUser.fromRequest(request).map(_.id)
     paypalService.executePayment(paymentId, payerId) match {
          case Right(executedPayment) =>
          paypalService.storeMetaData(paymentId, chosenVariants, cmp, intCmp, ophanId, idUser)
          getEmail(executedPayment) match {
              //TODO MOVE REDIRECT WITH PARAMS SOMEWHERE AND SHARE IT WITH THE GIRAFFE CONTROLLER
          case Some(email) => Redirect(routes.Giraffe.postPayment(countryGroup).url, queryParams, SEE_OTHER).withSession(request.session + ("email" -> email))
          case None => Redirect(routes.Giraffe.thanks(countryGroup).url, queryParams, SEE_OTHER)
        }
      case Left(error) => handleError(countryGroup, s"Error executing PayPal payment: $error")
    }
  }

  private def getEmail(payment: Payment): Option[String] = {
    for {
      payer <- Option(payment.getPayer)
      payerInfo <- Option(payer.getPayerInfo)
      email <- Option(payerInfo.getEmail)
    }
      yield {
        email
      }
  }

  case class AuthRequest(
    countryGroup: CountryGroup,
    amount: BigDecimal,
    cmp: Option[String],
    intCmp: Option[String],
    ophanId: Option[String]
  )

  object AuthRequest {
    implicit val authRequestReads: Reads[AuthRequest] = (
      (__ \ "countryGroup").read[CountryGroup] and
        (__ \ "amount").read(min[BigDecimal](1)) and
        (__ \ "cmp").readNullable[String] and
        (__ \ "intCmp").readNullable[String] and
        (__ \ "ophanId").readNullable[String]
      ) (AuthRequest.apply _)
  }

  case class AuthResponse(approvalUrl:String)

  implicit val AuthResponseWrites = Json.writes[AuthResponse]

  implicit val CountryGroupReads = new Reads[CountryGroup] {
    override def reads(json: JsValue): JsResult[CountryGroup] = json match {
      case JsString(id) => CountryGroup.byId(id).map(JsSuccess(_)).getOrElse(JsError("invalid CountryGroup id"))
      case _ => JsError("invalid value for country group")
    }
  }

  private def capAmount(amount: BigDecimal, currency: Currency): BigDecimal = amount min MaxAmount.forCurrency(currency)

  def authorize = NoCacheAction(parse.json) { request =>
    request.body.validate[AuthRequest] match {
      case JsSuccess(authRequest, _) =>
        val paypalService = paymentServices.paypalServiceFor(request)
        val authResponse = paypalService.getAuthUrl(
          amount = capAmount(authRequest.amount, authRequest.countryGroup.currency),
          countryGroup = authRequest.countryGroup,
          contributionId = ContributionIdGenerator.getNewId,
          cmp = authRequest.cmp,
          intCmp = authRequest.intCmp,
          ophanId = authRequest.ophanId
        )
        authResponse match {
          case Right(url) => Ok(Json.toJson(AuthResponse(url)))
          case Left(error) =>
            Logger.error(s"Error getting PayPal auth url: $error")
            InternalServerError("Error getting PayPal auth url")
        }
      case JsError(error) =>
        Logger.error(s"Invalid request=$error")
        BadRequest(s"Invalid request=$error")
    }
  }

  def handleError(countryGroup: CountryGroup, error: String) = {
    Logger.error(error)
    Redirect(routes.Giraffe.contribute(countryGroup, Some(PaypalError)).url, SEE_OTHER)
  }

  def hook = NoCacheAction(BodyParsers.parse.tolerantText) { request =>
    val bodyText = request.body
    val bodyJson = Json.parse(request.body)

    val paypalService = paymentServices.paypalServiceFor(request)
    val validHook = paypalService.validateEvent(request.headers.toSimpleMap, bodyText)

    bodyJson.validate[PaymentHook] match {
      case JsSuccess(paymentHook, _) if validHook =>
        paypalService.processPaymentHook(paymentHook)
        Logger.info(s"Received paymentHook: $paymentHook")
        Ok
      case JsError(errors) =>
        Logger.error(s"Unable to parse Json, parsing errors: $errors")
        InternalServerError("Unable to parse json payload")
      case _ =>
        Logger.error(s"A webhook request wasn't valid: $request, headers: ${request.headers.toSimpleMap},body: $bodyText")
        Forbidden("Request isn't signed by Paypal")
    }
  }
  case class MetadataUpdate(marketingOptIn: Boolean)

  val metadataUpdateForm: Form[MetadataUpdate] = Form(
    mapping(
      "marketingOptIn"->boolean
    )(MetadataUpdate.apply)(MetadataUpdate.unapply)
  )


  def updateMetadata(countryGroup: CountryGroup) = NoCacheAction.async { implicit request =>
    val paypalService = paymentServices.paypalServiceFor(request)
    metadataUpdateForm.bindFromRequest().fold[Future[Result]]({ withErrors =>
      Future.successful(BadRequest(JsArray(withErrors.errors.map(k => JsString(k.key)))))
    }, { f =>
      request.session.data.get("email") match {
        case Some(email) => paypalService.updateMarketingOptIn(email, f.marketingOptIn)
        case None => Logger.error("email not found in session while trying to update marketing opt in")
      }
      Future.successful(Redirect(routes.Giraffe.thanks(countryGroup).url, SEE_OTHER))
    })
  }


}
