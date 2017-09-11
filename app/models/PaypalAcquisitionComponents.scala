package models

import abtests.Allocation
import actions.CommonActions.ABTestRequest
import com.gu.acquisition.services.OphanServiceError
import com.paypal.api.payments.Payment
import controllers.httpmodels.CaptureRequest
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event._
import services.ContributionOphanService.{AcquisitionSubmissionBuilder, AcquisitionSubmissionBuilderUtils, OphanIds}

object PaypalAcquisitionComponents {

  case class Execute(payment: Payment, request: Execute.RequestData)

  object Execute {

    case class RequestData(
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
      abTest: Option[AbTest],
      testAllocations: Set[Allocation]
    )

    implicit object paypalAcquisitionSubmissionBuilder
      extends AcquisitionSubmissionBuilder[Execute] with AcquisitionSubmissionBuilderUtils {

      def buildOphanIds(components: Execute): Either[OphanServiceError, OphanIds] = {
        import components._
        for {
          browserId <- tryField("ophanBrowserId")(request.ophanBrowserId.get)
          pageviewId <- tryField("ophanPageviewId")(request.ophanPageviewId.get)
        } yield OphanIds(browserId, pageviewId, request.ophanVisitId)
      }

      def buildAcquisition(components: Execute): Either[OphanServiceError, Acquisition] = {
        import components._
        for {
          amount <- tryField("amount")(payment.getPaymentInstruction.getAmount.getValue.toDouble)
        } yield {
          Acquisition(
            product = Product.Contribution,
            paymentFrequency = PaymentFrequency.OneOff,
            currency = payment.getPaymentInstruction.getAmount.getCurrency,
            amount = amount,
            amountInGBP = None, // Calculated at the sinks of the Ophan stream
            paymentProvider = Some(ophan.thrift.event.PaymentProvider.Paypal),
            campaignCode = Some(Set(request.intCmp, request.cmp).flatten),
            abTests = Some(abTestInfo(request.testAllocations, request.abTest)),
            countryCode = Some(payment.getPayer.getPayerInfo.getCountryCode),
            referrerPageViewId = request.refererPageviewId,
            referrerUrl = request.refererUrl,
            componentId = request.componentId,
            componentTypeV2 = request.componentType,
            source = request.source
          )
        }
      }
    }
  }

  case class Capture(payment: Payment, request: ABTestRequest[CaptureRequest])

  object Capture {

    implicit object paypalAcquisitionSubmissionBuilder
      extends AcquisitionSubmissionBuilder[Capture] with AcquisitionSubmissionBuilderUtils {

      override def buildOphanIds(components: Capture): Either[OphanServiceError, OphanIds] = {
        import components._
        for {
          browserId <- tryField("ophanBrowserId")(request.body.ophanBrowserId.get)
          pageviewId <- tryField("ophanPageviewId")(request.body.ophanPageviewId.get)
        } yield OphanIds(browserId, pageviewId, visitId = None)
      }

      override def buildAcquisition(components: Capture): Either[OphanServiceError, Acquisition] = {
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
            abTests = Some(abTestInfo(request.testAllocations, request.body.abTest)),
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
