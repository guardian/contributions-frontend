package models

import actions.CommonActions.MetaDataRequest
import com.gu.stripe.Stripe.Charge
import controllers.forms.ContributionRequest
import ophan.thrift.event.{Acquisition, PaymentFrequency, Product}
import services.ContributionOphanService.{ContributionAcquisitionSubmissionBuilder, OphanIds}

case class StripeAcquisitionComponents(charge: Charge, request: MetaDataRequest[ContributionRequest])

object StripeAcquisitionComponents {

  implicit object stripeAcquisitionSubmissionBuilder
    extends ContributionAcquisitionSubmissionBuilder[StripeAcquisitionComponents] {

    def buildOphanIds(components: StripeAcquisitionComponents): Either[String, OphanIds] = {
      import components._
      attemptToGet("ophan browser id")(request.body.ophanBrowserId.get).map { browserId =>
        OphanIds(browserId, request.body.ophanPageviewId, request.body.ophanVisitId)
      }
    }

    override def buildAcquisition(components: StripeAcquisitionComponents): Either[String, Acquisition] = {
      import components._
      Either.right(
        Acquisition(
          product = Product.Contribution,
          paymentFrequency = PaymentFrequency.OneOff,
          currency = charge.currency,
          // Stripe amount in smallest currency unit.
          // Convert e.g. Pence to Pounds, Cents to Dollars
          // https://stripe.com/docs/api#charge_object
          amount = BigDecimal(charge.amount, 2).toDouble,
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
  }
}
