package monitoring

import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import configuration.Config
import play.api.libs.json.Json

trait ContributionMetrics extends TagAwareLogger {

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

    CloudWatch.cloudwatch.putMetricDataAsync(request, CloudWatch.LoggingAsyncHandler)
  }

  def put(metricName: String): Unit = {
    put(metricName, 1)
  }

  def logStripePaymentAttempt(platform: String): Unit = {
    put(s"contribution-stripe-payment-attempt-from-$platform")
  }

  def logStripePaymentSuccess(platform: String): Unit = {
    put(s"contribution-stripe-payment-success-from-$platform")
  }

  def logStripeAppThankYou(platform: String): Unit = {
    put(s"contribution-stripe-app-thank-you-$platform")
  }

  def logStripePaymentFailure(platform: String, error: String): Unit = {
    put(s"contribution-stripe-payment-failure-from-$platform-error-code-$error")
  }

  def logStripeHookSuccess(): Unit = {
    put("contribution-stripe-hook-success")
  }

  def logStripeHookParseError(): Unit = {
    put("contribution-stripe-hook-parse-error")
  }

  def logStripeHookFailure(): Unit = {
    put("contribution-stripe-hook-failure")
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

  def logPaypalPaymentFailure(message: String): Unit = {
    val failureMessage = Json.parse(message)
    val errorName = Option((failureMessage \\ "name").toString()).getOrElse("Unknown")
    put(s"contribution-paypal-payment-failed-error-$errorName")
  }

  def logPaypalHookAttempt(): Unit = {
    put("contribution-paypal-hook-success")
  }

  def logPaypalHookSuccess(): Unit = {
    put("contribution-paypal-hook-success")
  }

  def logPaypalHookParseError(): Unit = {
    put("contribution-paypal-hook-parse-error")
  }

  def logPaypalHookFailure(): Unit = {
    put("contribution-paypal-hook-failure")
  }

  def logPaypalPostPaymentPage(): Unit = {
    put("contribution-paypal-post-payment-page")
  }

  def logThankYouPage(): Unit = {
    put("contribution-thank-you-page")
  }


}
