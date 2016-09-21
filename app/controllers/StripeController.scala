package controllers

import actions.CommonActions._
import cats.data.{Xor, XorT}
import cats.implicits._
import com.typesafe.config.Config
import models.{PaymentHook, SavedContributionData, StripeHook}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{BodyParsers, Controller, Result}
import services.PaymentServices

import scala.concurrent.{ExecutionContext, Future}

class StripeController(paymentServices: PaymentServices, stripeConfig: Config)(implicit ec: ExecutionContext) extends Controller {

  val webhookKey = stripeConfig.getString("webhook.key")

  def hook = SharedSecretAction(webhookKey) {
    NoCacheAction.async(BodyParsers.parse.json) { request =>
      val stripeService = paymentServices.stripeServiceFor(request)
      val idUser = IdentityUser.fromRequest(request).map(_.id)

      def savePaymentHook(stripeHook: StripeHook)(savedContributionData: SavedContributionData): XorT[Future, String, PaymentHook] = {
        val contributionId = savedContributionData.contributionMetaData.contributionId
        stripeService.processPaymentHook(stripeHook, contributionId)
      }

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
        stripeService.storeMetaData(stripeHook, idUser)
          .flatMap(savePaymentHook(stripeHook))
          .value.map {
          case Xor.Right(_) => Ok
          case Xor.Left(_) => InternalServerError
        }
      }
    }
  }
}
