package monitoring

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import configuration.Config
import play.api.libs.json.Json


/**
  * Created by mmcnamara on 12/07/2017.
  */
class CloudWatchMetrics(cloudWatchClient: AmazonCloudWatchAsync) extends TagAwareLogger {

  val stage: String = Config.stage
  val application = "contributions" // This sets the namespace for Custom Metrics in AWS (see CloudWatch)

  val stageDimension: Dimension = new Dimension().withName("Stage").withValue(stage)

  def put(name: String, count: Double): Unit = {
    val metric =
      new MetricDatum()
        .withValue(count)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions(stageDimension)

    val request = new PutMetricDataRequest().
      withNamespace(application).withMetricData(metric)

    cloudWatchClient.putMetricDataAsync(request, CloudWatch.LoggingAsyncHandler)
  }

  def put(metricName: String): Unit = {
    put(metricName, 1)
  }

  def logHomePage(): Unit = {
    put("contribution-home-page")
  }
  def logPaypalPostPaymentPage(): Unit = {
    put("contribution-paypal-post-payment-page")
  }
  def logThankYouPageDisplayed(paymentMethod: String): Unit = {
    put(s"contribution-thank-you-page-displayed-$paymentMethod")
  }


  def logStripePaymentAttempt(platform: String): Unit = {
    put(s"contribution-stripe-payment-attempt-from-$platform")
  }

  def logStripePaymentSuccess(platform: String): Unit = {
    put(s"contribution-stripe-payment-success-from-$platform")
  }
  
  def logStripeSuccessRedirected(platform: String): Unit = {
    put(s"contribution-stripe-payment-success-redirected-to-$platform")
  }

  def logStripePaymentFailure(platform: String): Unit = {
    put(s"contribution-stripe-payment-failure-from-$platform")
  }

  def logStripeHookParsed(): Unit = {
    put("contribution-stripe-hook-parsed")
  }

  def logStripeHookParseError(): Unit = {
    put("contribution-stripe-hook-parse-error")
  }

  def logStripeHookProcessed(): Unit = {
    put("contribution-stripe-hook-processed")
  }

  def logStripeHookProcessError(): Unit = {
    put("contribution-stripe-hook-processing-error")
  }


  def logPaypalAuthAttempt(): Unit = {
    put("contribution-paypal-authorisation-attempt")
  }

  def logPaypalAuthSuccess(): Unit = {
    put("contribution-paypal-authorisation-success")
  }

  def logPaypalAuthFailure(): Unit = {
    put("contribution-paypal-authorisation-failure")
  }

  def logPaypalPaymentAttempt(): Unit = {
    put("contribution-paypal-payment-attempt")
  }

  def logPaypalPaymentSuccess(): Unit = {
    put("contribution-paypal-payment-success")
  }

  def logPaypalPaymentFailure(): Unit = {
    put("contribution-paypal-payment-failed-error")
  }

  def logPaypalHookAttempt(): Unit = {
    put("contribution-paypal-hook-attempt")
  }

  def logPaypalHookParsed(): Unit = {
    put("contribution-paypal-hook-parsed")
  }

  def logPaypalHookProcessed(): Unit = {
    put("contribution-paypal-hook-processed")
  }

  def logPaypalHookParseError(): Unit = {
    put("contribution-paypal-hook-parse-error")
  }

  def logPaypalHookProcessError(): Unit = {
    put("contribution-paypal-hook-processing-error")
  }

  def logPaypalHookInvalidRequest(): Unit = {
    put("contribution-paypal-hook-invalid-request")
  }

}
