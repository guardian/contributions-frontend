package monitoring

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import com.gu.zuora.soap.models.Commands.PaymentMethod
import configuration.Config
import play.api.libs.json.Json

import scala.tools.nsc.backend.Platform


/**
  * Created by mmcnamara on 12/07/2017.
  */
class CloudWatchMetrics(cloudWatchClient: AmazonCloudWatchAsync) extends TagAwareLogger {

  val stage: String = Config.stage
  val application = "contributions" // This sets the namespace for Custom Metrics in AWS (see CloudWatch)

  val stageDimension: Dimension = new Dimension().withName("Stage").withValue(stage)

  def put(name: String, paymentMethod: String = "unknown", platform: String = "web", count: Double = 1): Unit = {
    val platformDimension = new Dimension().withName("platform").withValue(platform)
    val paymentMethodDimension = new Dimension().withName("paymentMethod").withValue(paymentMethod)
    val metric =
      new MetricDatum()
        .withValue(count)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions(stageDimension, platformDimension, paymentMethodDimension)

    val request = new PutMetricDataRequest().
      withNamespace(application).withMetricData(metric)

    cloudWatchClient.putMetricDataAsync(request, CloudWatch.LoggingAsyncHandler)
  }


  def logHomePage(): Unit = {
    put("home-page-displayed")
  }

  def logPaymentAuthAttempt(paymentMethod: String = "paypal", platform: String = "web"): Unit = {
    put("payment-authorisation-attempt", paymentMethod, platform)
  }

  def logPaymentAuthSuccess(paymentMethod: String = "paypal", platform: String = "web"): Unit = {
    put("payment-authorisation-success", paymentMethod, platform)
  }

  def logPaymentAuthFailure(paymentMethod: String = "paypal", platform: String = "web"): Unit = {
    put("payment-authorisation-failure", paymentMethod, platform)
  }

  def logPaymentAttempt(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-attempt", paymentMethod, platform)
  }

  def logPaymentSuccess(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-success",paymentMethod, platform)
  }

  def logPaymentSuccessRedirected(paymentMethod: String = "stripe", platform: String): Unit = {
    put("payment-success-redirected-to-other-platform", paymentMethod, platform)
  }

  def logPaymentFailure(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-failure", paymentMethod, platform)
  }

  def logHookAttempt(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-hook-attempt", paymentMethod, platform)
  }

  def logHookParsed(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-hook-parsed", paymentMethod, platform)
  }

  def logHookInvalidRequest(paymentMethod: String, platform: String = "web"): Unit = {
    put("contribution-paypal-hook-invalid-request", paymentMethod, platform)
  }

  def logHookParseError(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-hook-parse-error", paymentMethod, platform)
  }

  def logHookProcessed(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-hook-processed", paymentMethod, platform)
  }

  def logHookProcessError(paymentMethod: String, platform: String = "web"): Unit = {
    put("payment-hook-processing-error", paymentMethod, platform)
  }

  def logPostPaymentPageDisplayed(): Unit = {
    put("post-payment-page-displayed")
  }

  def logThankYouPageDisplayed(paymentMethod: String, platform: String = "web"): Unit = {
    put("thank-you-page-displayed", paymentMethod, platform)
  }

}
