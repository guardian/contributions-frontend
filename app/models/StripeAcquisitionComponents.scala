package models

import actions.CommonActions.ABTestRequest
import com.gu.acquisition.services.OphanServiceError
import com.gu.stripe.Stripe.Charge
import controllers.forms.ContributionRequest
import ophan.thrift.event.{Acquisition, PaymentFrequency, Product}
import services.ContributionOphanService.{AcquisitionSubmissionBuilder, AcquisitionSubmissionBuilderUtils, OphanIds}

case class StripeAcquisitionComponents(charge: Charge, request: ABTestRequest[ContributionRequest])

object StripeAcquisitionComponents {

  implicit object stripeAcquisitionSubmissionBuilder
    extends AcquisitionSubmissionBuilder[StripeAcquisitionComponents] with AcquisitionSubmissionBuilderUtils {

    override def buildAcquisition(components: StripeAcquisitionComponents): Either[OphanServiceError, Acquisition] = {
      import components._
      Either.right(
        Acquisition(
          product = Product.Contribution,
          paymentFrequency = PaymentFrequency.OneOff,
          currency = charge.currency,
          amount = charge.amount,
          amountInGBP = None, // Calculated at the sinks of the Ophan stream
          paymentProvider = Option(ophan.thrift.event.PaymentProvider.Stripe),
          campaignCode = Some(Set(request.body.intcmp, request.body.cmp).flatten),
          abTests = Some(abTestInfo(request.testAllocations, request.body.abTest)),
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
