package services

import com.gu.identity.play.AuthenticatedIdUser
import com.gu.identity.testing.usernames.TestUsernames
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeCredentials, StripeService}
import com.typesafe.config.Config
import data.ContributionData
import play.api.mvc.RequestHeader
import services.PaymentServices.{Default, Mode, Testing}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global


object PaymentServices {

  /* https://stripe.com/docs/dashboard#livemode-and-testing
   */
  sealed trait Mode {
    val name: String
  }

  case object Default extends Mode {
    val name = "default"
  }

  case object Testing extends Mode {
    val name = "testing"
  }

  object Mode {
    val all: Set[Mode] = Set(Default, Testing)
  }


  def stripeServiceFor(stripeConfig: Config, universe: Mode): StripeService = {
    val stripeMode = stripeConfig.getString(universe.name)
    val keys = stripeConfig.getConfig(s"keys.$stripeMode")
    val credentials = StripeCredentials(
      secretKey = keys.getString("secret"),
      publicKey = keys.getString("public")
    )

    val metrics = new ServiceMetrics(stripeMode, "giraffe","stripe")
    new StripeService(StripeApiConfig(stripeMode, credentials), metrics)
  }

  def stripeServicesFor(stripeConfig: Config):Map[Mode, StripeService]  = Mode.all.map(mode => mode -> stripeServiceFor(stripeConfig, mode)).toMap

  def paypalServiceFor(paypalConfig: Config, universe: Mode, contributionData: ContributionData)(ec: ExecutionContext): PaypalService = {
    val paypalMode = paypalConfig.getString(universe.name)
    val keys = paypalConfig.getConfig(paypalMode)
    val apiConfig = PaypalApiConfig.from(keys, paypalMode)
    new PaypalService(apiConfig, contributionData)(ec)
  }


  def paypalServicesFor(paypalConfig: Config, contributionDataPerMode: Map[Mode, ContributionData])(ec: ExecutionContext): Map[Mode, PaypalService] =
    Mode.all.map(mode => mode -> paypalServiceFor(paypalConfig, mode, contributionDataPerMode(mode))(ec)).toMap
}

case class PaymentServices(
  authProvider: AuthenticatedIdUser.Provider,
  testUsernames: TestUsernames,
  stripeServices: Map[Mode, StripeService],
  paypalServices: Map[Mode, PaypalService]
) {

  private def isTestUser(request: RequestHeader): Boolean = authProvider(request).flatMap(_.displayName).exists(testUsernames.isValid)

  private def modeFor(request: RequestHeader): Mode = if (isTestUser(request)) Testing else Default

  def stripeServiceFor(request: RequestHeader): StripeService = stripeServices(modeFor(request))

  def paypalServiceFor(request: RequestHeader): PaypalService = paypalServices(modeFor(request))

}
