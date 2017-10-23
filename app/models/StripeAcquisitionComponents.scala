package models

import actions.CommonActions.MetaDataRequest
import com.gu.acquisition.model.OphanIds
import com.gu.stripe.Stripe.Charge
import controllers.forms.ContributionRequest
import ophan.thrift.event.{Acquisition, PaymentFrequency, Product}
import services.ContributionOphanService.ContributionAcquisitionSubmissionBuilder

case class StripeAcquisitionComponents(charge: Charge, request: MetaDataRequest[ContributionRequest])

object StripeAcquisitionComponents {

  implicit object stripeAcquisitionSubmissionBuilder
    extends ContributionAcquisitionSubmissionBuilder[StripeAcquisitionComponents] {

    def buildOphanIds(components: StripeAcquisitionComponents): Either[String, OphanIds] = {
      import components._
      Right(OphanIds(Some(request.body.ophanPageviewId), request.body.ophanVisitId, request.body.ophanBrowserId))
    }

    override def buildAcquisition(components: StripeAcquisitionComponents): Either[String, Acquisition] = {
      import components._
      Right(
        Acquisition(
          product = Product.Contribution,
          paymentFrequency = PaymentFrequency.OneOff,
          currency = charge.currency,
          // Stripe amount is in smallest currency unit.
          // Convert e.g. Pence to Pounds, Cents to Dollars
          // https://stripe.com/docs/api#charge_object
          amount = BigDecimal(charge.amount, 2).toDouble,
          paymentProvider = Option(ophan.thrift.event.PaymentProvider.Stripe),
          campaignCode = Some(Set(request.body.intcmp, request.body.cmp).flatten),
          abTests = Some(abTestInfo(request.body.nativeAbTests, request.body.refererAbTest)),
          countryCode = Some(charge.source.country),
          referrerPageViewId = request.body.refererPageviewId,
          referrerUrl = request.body.refererUrl,
          componentId = request.body.componentId,
          componentTypeV2 = request.body.componentType,
          source = request.body.source,
          platform = Some(ophan.thrift.event.Platform.Contribution)
        )
      )
    }
  }
}
