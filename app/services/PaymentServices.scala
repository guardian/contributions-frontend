package services

import com.gu.identity.play.AuthenticatedIdUser
import com.gu.identity.testing.usernames.TestUsernames
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeCredentials}
import com.typesafe.config.Config
import data.ContributionData
import models.PaymentMode
import models.PaymentMode.{Default, Testing}
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global


object PaymentServices {

  def stripeServiceFor(stripeConfig: Config, universe: PaymentMode, contributionData: ContributionData): StripeService = {
    val stripeMode = stripeConfig.getString(universe.name)
    val keys = stripeConfig.getConfig(s"keys.$stripeMode")
    val credentials = StripeCredentials(
      secretKey = keys.getString("secret"),
      publicKey = keys.getString("public")
    )

    val metrics = new ServiceMetrics(stripeMode, "giraffe","stripe")
    new StripeService(StripeApiConfig(stripeMode, credentials), metrics, contributionData)
  }

  def stripeServicesFor(stripeConfig: Config, contributionDataPerMode: Map[PaymentMode, ContributionData]):Map[PaymentMode, StripeService] = {
    PaymentMode.all.map(mode => mode -> stripeServiceFor(stripeConfig, mode, contributionDataPerMode(mode))).toMap
  }

  def paypalServicesFor(paypalConfig: Config, contributionDataPerMode: Map[PaymentMode, ContributionData])(ec: ExecutionContext): Map[PaymentMode, PaypalService] = {

    def paypalServiceFor(universe: PaymentMode): PaypalService = {
      val contributionData = contributionDataPerMode(universe)
      val paypalMode = paypalConfig.getString(universe.name)
      val keys = paypalConfig.getConfig(paypalMode)
      val apiConfig = PaypalApiConfig.from(keys, paypalMode)
      new PaypalService(apiConfig, contributionData)(ec)
    }

    PaymentMode.all.map(mode => mode -> paypalServiceFor(mode)).toMap
  }
}

case class PaymentServices(
  authProvider: AuthenticatedIdUser.Provider,
  testUsernames: TestUsernames,
  stripeServices: Map[PaymentMode, StripeService],
  paypalServices: Map[PaymentMode, PaypalService]
) {

  private def isTestUser(request: RequestHeader): Boolean = authProvider(request).flatMap(_.displayName).exists(testUsernames.isValid)

  private def modeFor(request: RequestHeader): PaymentMode = if (isTestUser(request)) Testing else Default

  def stripeServiceFor(request: RequestHeader): StripeService = stripeServices(modeFor(request))

  def paypalServiceFor(request: RequestHeader): PaypalService = paypalServices(modeFor(request))

}
