package services

import actions.CommonActions.ABTestRequest
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherT
import cats.syntax.either._
import com.gu.acquisition.services.{OphanService, OphanServiceError}
import com.gu.stripe.Stripe.Charge
import com.paypal.api.payments.Payment
import controllers.forms.ContributionRequest
import controllers.httpmodels.CaptureRequest
import ophan.thrift.event.Acquisition
import play.api.{Environment, Mode}
import services.ContributionOphanService.{AcquisitionSubmissionBuilder, OphanIds}
import simulacrum.typeclass

import scala.concurrent.{ExecutionContext, Future}

/**
  * Used to send contribution acquisitions to Ophan.
  */
class ContributionOphanService(env: Environment)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  /**
    * A right-valued result is returned iff one of the following events occurred:
    * (1) the app is not in production mode; (2) the app is in production mode and the event was successfully sent.
    */
  // TODO: should an error be returned if not in production mode?
  def submitAcquisition[A : AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext): EitherT[Future, OphanServiceError, Unit] = {
    import cats.instances.future._
    import ContributionOphanService._
    import AcquisitionSubmissionBuilder.ops._

    EitherT.fromEither[Future](a.asAcquisitionSubmission)
      .flatMap {
        // Ophan should filter out events coming from the Guardian IP addresses.
        // Still, explicitly check the server is running in production mode as extra precaution,
        // before submitting the event.
        case AcquisitionSubmission(ophanIds, acquisition) if env.mode == Mode.Prod =>
          OphanService.submit(acquisition, ophanIds.browserId, ophanIds.viewId, ophanIds.visitId).map(_ => ())
        case _ =>
          EitherT.pure[Future, OphanServiceError, Unit](())
      }
  }
}

object ContributionOphanService {

  case class OphanIds(browserId: String, viewId: String, visitId: Option[String])

  /**
    * Encapsulates all the data required to submit an acquisition to Ophan.
    */
  case class AcquisitionSubmission(ophanIds: OphanIds, acquisition: Acquisition)

  /**
    * Type class for creating an acquisition submission from an arbitrary data type.
    */
  @typeclass trait AcquisitionSubmissionBuilder[A] {

    def buildOphanIds(a: A): Either[OphanServiceError, OphanIds]

    def buildAcquisition(a: A): Either[OphanServiceError, Acquisition]

    def asAcquisitionSubmission(a: A): Either[OphanServiceError, AcquisitionSubmission] =
      for {
        ophanIds <- buildOphanIds(a)
        acquisition <- buildAcquisition(a)
      } yield AcquisitionSubmission(ophanIds, acquisition)
  }
}

/**
  * Mixin to facilitate creating acquisition submission builders.
  */
trait OphanServiceErrorUtils {

  def tryField[A](name: String)(a: => A): Either[OphanServiceError, A] =
    Either.catchNonFatal(a).leftMap { _ =>
      OphanServiceError.Generic(new RuntimeException(s"unable to get value for field $name"))
    }
}

case class StripeAcquisitionComponents(charge: Charge, request: ABTestRequest[ContributionRequest])

object StripeAcquisitionComponents {

  implicit object stripeAcquisitionSubmissionBuilder
    extends AcquisitionSubmissionBuilder[StripeAcquisitionComponents]
    with OphanServiceErrorUtils {

    override def buildAcquisition(components: StripeAcquisitionComponents): Either[OphanServiceError, Acquisition] = {
      import com.gu.acquisition.syntax._
      import components._

      Either.right(
        Acquisition(
          product = ophan.thrift.event.Product.Contribution,
          paymentFrequency = ophan.thrift.event.PaymentFrequency.OneOff,
          currency = charge.currency,
          amount = charge.amount,
          amountInGBP = None, // Calculated at the sinks of the Ophan stream
          paymentProvider = Option(ophan.thrift.event.PaymentProvider.Stripe),
          campaignCode = Some(Set(request.body.intcmp, request.body.cmp).flatten),
          abTests = Some(components.request.testAllocations.asAbTestInfo),
          countryCode = Some(charge.source.country),
          referrerPageViewId = request.body.refererPageviewId,
          referrerUrl = request.body.refererUrl,
          componentId = request.body.componentId,
          componentTypeV2 = request.body.componentType,
          source = request.body.source
        )
      )
    }

    def buildOphanIds(components: StripeAcquisitionComponents): Either[OphanServiceError, OphanIds] = {
      import components._
      tryField("ophanBrowserId")(request.body.ophanBrowserId.get)
        .map { browserId =>
          OphanIds(browserId, request.body.ophanPageviewId, request.body.ophanVisitId)
        }
    }
  }
}

object PaypalAcquisitionComponents {

  case class Execute(payment: Payment, queryStringFields: Execute.QueryStringFields)

  object Execute {

    case class QueryStringFields(
        cmp: Option[String],
        intCmp: Option[String],
        refererPageviewId: Option[String],
        refererUrl: Option[String],
        ophanPageviewId: Option[String],
        ophanBrowserId: Option[String],
        ophanVisitId: Option[String]
    )

    implicit object paypalAcquisitionSubmissionBuilder
      extends AcquisitionSubmissionBuilder[Execute] with OphanServiceErrorUtils {

      def buildOphanIds(components: Execute): Either[OphanServiceError, OphanIds] = {
        import components._
        for {
          browserId <- tryField("ophanBrowserId")(queryStringFields.ophanBrowserId.get)
          pageviewId <- tryField("ophanPageviewId")(queryStringFields.ophanPageviewId.get)
        } yield OphanIds(browserId, pageviewId, queryStringFields.ophanVisitId)
      }

      def buildAcquisition(components: Execute): Either[OphanServiceError, Acquisition] = {
        import components._
        for {
          amount <- tryField("amount")(payment.getPaymentInstruction.getAmount.getValue.toDouble)
        } yield {
          Acquisition(
            product = ophan.thrift.event.Product.Contribution,
            paymentFrequency = ophan.thrift.event.PaymentFrequency.OneOff,
            currency = payment.getPaymentInstruction.getAmount.getCurrency,
            amount = amount,
            amountInGBP = None, // Calculated at the sinks of the Ophan stream
            paymentProvider = Some(ophan.thrift.event.PaymentProvider.Paypal),
            campaignCode = Some(Set(queryStringFields.intCmp, queryStringFields.cmp).flatten),
            abTests = None, // TODO: pass through ab tests as query parameter
            countryCode = Some(payment.getPayer.getPayerInfo.getCountryCode),
            referrerPageViewId = queryStringFields.refererPageviewId,
            referrerUrl = queryStringFields.refererUrl,
            componentId = None, // TODO: pass through component id as query parameter
            componentTypeV2 = None, // TODO: pass through component type v2 as query parameter
            source = None // TODO: pass through source as query parameter
          )
        }
      }
    }
  }

  case class Capture(payment: Payment, request: ABTestRequest[CaptureRequest])

  object Capture {

    implicit object paypalAcquisitionSubmissionBuilder
      extends AcquisitionSubmissionBuilder[Capture] with OphanServiceErrorUtils {

      override def buildOphanIds(components: Capture): Either[OphanServiceError, OphanIds] = {
        import components._
        for {
          browserId <- tryField("ophanBrowserId")(request.body.ophanBrowserId.get)
          pageviewId <- tryField("ophanPageviewId")(request.body.ophanPageviewId.get)
        } yield OphanIds(browserId, pageviewId, visitId = None)
      }

      override def buildAcquisition(components: Capture): Either[OphanServiceError, Acquisition] = {
        import com.gu.acquisition.syntax._
        import components._

        for {
          amount <- tryField("amount")(payment.getPaymentInstruction.getAmount.getValue.toDouble)
        } yield {
          Acquisition(
            product = ophan.thrift.event.Product.Contribution,
            paymentFrequency = ophan.thrift.event.PaymentFrequency.OneOff,
            currency = payment.getPaymentInstruction.getAmount.getCurrency,
            amount = amount,
            amountInGBP = None, // Calculated at the sinks of the Ophan stream
            paymentProvider = Some(ophan.thrift.event.PaymentProvider.Paypal),
            campaignCode = Some(Set(request.body.cmp, request.body.intCmp).flatten),
            abTests = Some(request.testAllocations.asAbTestInfo),
            countryCode = Some(payment.getPayer.getPayerInfo.getCountryCode),
            referrerPageViewId = request.body.refererPageviewId,
            referrerUrl = request.body.refererUrl,
            componentId = request.body.componentId,
            componentTypeV2 = request.body.componentType,
            source = request.body.source
          )
        }
      }
    }
  }
}

