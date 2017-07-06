package monitoring

import configuration.Config
import play.api.libs.json.Json

trait ContributionMetrics extends CloudWatch{

  val stage: String = Config.stage
  val application = "contributions" // This sets the namespace for Custom Metrics in AWS (see CloudWatch)

  def putStripePaymentAttempt(platform: String): Unit ={
    put(s"contribution-stripe-payment-attempt-from-$platform")
  }

  def putStripePaymentSuccess(platform: String): Unit ={
    put(s"contribution-stripe-payment-success-from-$platform")
  }

  def putStripeAppThankYou(platform: String): Unit ={
    put(s"contribution-stripe-app-thank-you-$platform")
  }

  def putStripePaymentFailure(platform: String, error: String): Unit ={
    put(s"contribution-stripe-payment-failure-from-$platform-error-code-$error")
  }

  def putStripeHookSuccess(): Unit ={
    put(s"contribution-stripe-hook-success")
  }

  def putStripeHookParseError(): Unit ={
    put(s"contribution-stripe-hook-parse-error")
  }

  def putStripeHookFailure(): Unit ={
    put(s"contribution-stripe-hook-failure")
  }

  def putPaypalAuthAttempt(): Unit ={
    put(s"contribution-paypal-authorisation-attempt")
  }

  def putPaypalAuthSuccess(): Unit ={
    put(s"contribution-paypal-authorisation-success")
  }

  def putPaypalAuthFailure(): Unit ={
    put(s"contribution-paypal-authorisation-failure")
  }

  def putPaypalPaymentAttempt(): Unit ={
    put(s"contribution-paypal-payment-attempt")
  }

  def putPaypalPaymentSuccess(): Unit ={
    put(s"contribution-paypal-payment-success")
  }

  def putPaypalPaymentFailure(message: String): Unit ={
    val failureMessage = Json.parse(message)
    val errorName = Option((failureMessage \\ "name").toString()).getOrElse("Unknown")
    put(s"contribution-paypal-payment-failed-error-$errorName")
  }

  def putPaypalHookAttempt(): Unit ={
    put(s"contribution-paypal-hook-success")
  }

  def putPaypalHookSuccess(): Unit ={
    put(s"contribution-paypal-hook-success")
  }

  def putPaypalHookParseError(): Unit ={
    put(s"contribution-paypal-hook-parse-error")
  }

  def putPaypalHookFailure(): Unit ={
    put(s"contribution-paypal-hook-failure")
  }

  def putPaypalPostPaymentPage(): Unit ={
    put(s"contribution-paypal-post-payment-page")
  }

  def putThankYouPage(): Unit ={
    put(s"contribution-thank-you-page")
  }


  private def put(metricName: String) {
   put(metricName, 1)
  }

}
