package monitoring

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import configuration.Config
import models.PaymentProvider


/**
  * Created by mmcnamara on 12/07/2017.
  */
class CloudWatchMetrics(cloudWatchClient: AmazonCloudWatchAsync) extends TagAwareLogger {
  import CloudWatchMetrics.DimensionValue

  val application = s"contributions-${Config.stage}" // This sets the namespace for Custom Metrics in AWS (see CloudWatch)


  private def put(name: String, paymentProvider: String, platform: String): Unit = {
    val platformDimension = new Dimension().withName("platform").withValue(platform)
    val paymentProviderDimension = new Dimension().withName("payment-provider").withValue(paymentProvider)

    val metric =
      new MetricDatum()
        .withValue(1d)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions(platformDimension, paymentProviderDimension)

    val request = new PutMetricDataRequest().withNamespace(application).withMetricData(metric)

    cloudWatchClient.putMetricDataAsync(request, CloudWatch.LoggingAsyncHandler)
  }

  private def put(name: String, paymentProvider: PaymentProvider, platform: String): Unit = {
    put(name, paymentProvider.entryName, platform)
  }

  def logHomePage(platform: String): Unit = {
    put("home-page-displayed", DimensionValue.notApplicable, platform)
  }

  def logPaymentAuthAttempt(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-authorisation-attempt", paymentProvider.entryName, platform)
  }

  def logPaymentAuthSuccess(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-authorisation-success", paymentProvider, platform)
  }

  def logPaymentAuthFailure(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-authorisation-failure", paymentProvider, platform)
  }

  def logPaymentAttempt(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-attempt", paymentProvider, platform)
  }

  def logPaymentSuccess(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-success", paymentProvider, platform)
  }

  def logPaymentSuccessRedirected(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-success-redirected-to-other-platform", paymentProvider, platform)
  }

  def logPaymentFailure(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-failure", paymentProvider, platform)
  }

  def logUnhandledPaymentFailure(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("unhandled-payment-failure", paymentProvider, platform)
  }

  def logHookAttempt(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-attempt", paymentProvider, platform)
  }

  def logHookParsed(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-parsed", paymentProvider, platform)
  }

  def logHookInvalidRequest(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("contribution-paypal-hook-invalid-request", paymentProvider, platform)
  }

  def logHookParseError(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-parse-error", paymentProvider, platform)
  }

  def logHookProcessed(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-processed", paymentProvider, platform)
  }

  def logHookProcessError(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-processing-error", paymentProvider, platform)
  }

  def logPostPaymentPageDisplayed(paymentProvider: Option[PaymentProvider], platform: String): Unit = {
    put("post-payment-page-displayed", paymentProvider.map(_.entryName).getOrElse(DimensionValue.unknown), platform)
  }

  def logThankYouPageDisplayed(paymentProvider: Option[PaymentProvider], platform: String): Unit = {
    put("thank-you-page-displayed", paymentProvider.map(_.entryName).getOrElse(DimensionValue.unknown), platform)
  }

}


object CloudWatchMetrics {

  object DimensionValue {
    val unknown = "unknown"
    val notApplicable = "na"
  }
}
