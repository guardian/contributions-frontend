package monitoring

import play.api.libs.json.Json

class ContributionMetrics extends Metrics{

  def putStripePaymentAttempt(platform: String): Unit ={
    put(s"contribution-stripe-payment-attempt-from-$platform")
  }

  def putStripePaymentSuccess(platform: String): Unit ={
    put(s"contribution-stripe-payment-success-from-$platform")
  }

  def putStripePaymentFailure(platform: String, error: String): Unit ={
    put(s"contribution-stripe-payment-failure-from-$platform-error-code-$error")
  }

  def putStripeHookSuccess: Unit ={
    put(s"contribution-stripe-hook-success")
  }

  def putStripeHookParseError: Unit ={
    put(s"contribution-stripe-hook-parse-error")
  }

  def putStripeHookFailure: Unit ={
    put(s"contribution-stripe-hook-failure")
  }

  def putPaypalAuthAttempt: Unit ={
    put(s"contribution-paypal-authorisation-attempt")
  }

  def putPaypalAuthSuccess: Unit ={
    put(s"contribution-paypal-authorisation-success")
  }

  def putPaypalAuthFailure: Unit ={
    put(s"contribution-paypal-authorisation-failure")
  }

  def putPaypalPaymentAttempt: Unit ={
    put(s"contribution-paypal-payment-attempt")
  }

  def putPaypalPaymentSuccess: Unit ={
    put(s"contribution-paypal-payment-success")
  }

  def putPaypalPaymentFailure(message: String): Unit ={
    val failureMessage = Json.parse(message)
    val errorName = Option((failureMessage \\ "name").toString()).getOrElse("Unknown")
    put(s"contribution-paypal-payment-failed-error-$errorName")
  }

  def putPaypalHookAttempt: Unit ={
    put(s"contribution-paypal-hook-success")
  }

  def putPaypalHookSuccess: Unit ={
    put(s"contribution-paypal-hook-success")
  }

  def putPaypalHookParseError: Unit ={
    put(s"contribution-paypal-hook-parse-error")
  }

  def putPaypalHookFailure: Unit ={
    put(s"contribution-paypal-hook-failure")
  }


  private def put(metricName: String) {
   put(metricName, 1)
  }

}
