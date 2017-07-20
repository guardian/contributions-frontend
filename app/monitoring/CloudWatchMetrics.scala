package monitoring

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import com.gu.zuora.soap.models.Commands.PaymentMethod
import configuration.Config
import models.PaymentProvider
import play.api.libs.json.Json

import scala.tools.nsc.backend.Platform


/**
  * Created by mmcnamara on 12/07/2017.
  */
class CloudWatchMetrics(cloudWatchClient: AmazonCloudWatchAsync) extends TagAwareLogger {

  val stage: String = Config.stage
  val application = "contributions" // This sets the namespace for Custom Metrics in AWS (see CloudWatch)

  val stageDimension: Dimension = new Dimension().withName("Stage").withValue(stage)

  def put(name: String, paymentProvider: Option[PaymentProvider], platform: String): Unit = {
    val platformDimension = new Dimension().withName("platform").withValue(platform)

    val metric =
      new MetricDatum()
        .withValue(1d)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions(stageDimension, platformDimension)


    paymentProvider.foreach { provider =>
      val paymentMethodDimension = new Dimension().withName("paymentMethod").withValue(provider.entryName)
      metric.setDimensions(java.util.Arrays.asList(paymentMethodDimension))
    }

    val request = new PutMetricDataRequest().withNamespace(application).withMetricData(metric)

    cloudWatchClient.putMetricDataAsync(request, CloudWatch.LoggingAsyncHandler)
  }


  def logHomePage(platform: String): Unit = {
    put("home-page-displayed", None, platform)
  }

  def logPaymentAuthAttempt(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-authorisation-attempt", Some(paymentProvider), platform)
  }

  def logPaymentAuthSuccess(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-authorisation-success", Some(paymentProvider), platform)
  }

  def logPaymentAuthFailure(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-authorisation-failure", Some(paymentProvider), platform)
  }

  def logPaymentAttempt(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-attempt", Some(paymentProvider), platform)
  }

  def logPaymentSuccess(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-success",Some(paymentProvider), platform)
  }

  def logPaymentSuccessRedirected(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-success-redirected-to-other-platform", Some(paymentProvider), platform)
  }

  def logPaymentFailure(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-failure", Some(paymentProvider), platform)
  }

  def logHookAttempt(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-attempt", Some(paymentProvider), platform)
  }

  def logHookParsed(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-parsed", Some(paymentProvider), platform)
  }

  def logHookInvalidRequest(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("contribution-paypal-hook-invalid-request", Some(paymentProvider), platform)
  }

  def logHookParseError(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-parse-error", Some(paymentProvider), platform)
  }

  def logHookProcessed(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-processed", Some(paymentProvider), platform)
  }

  def logHookProcessError(paymentProvider: PaymentProvider, platform: String): Unit = {
    put("payment-hook-processing-error", Some(paymentProvider), platform)
  }

  def logPostPaymentPageDisplayed(paymentProvider: Option[PaymentProvider], platform: String): Unit = {
    put("post-payment-page-displayed", paymentProvider, platform)
  }

  def logThankYouPageDisplayed(paymentProvider: Option[PaymentProvider], platform: String): Unit = {
    put("thank-you-page-displayed", paymentProvider, platform)
  }

}
