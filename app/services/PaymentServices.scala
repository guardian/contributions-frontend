package services

import akka.actor.ActorSystem
import com.gu.i18n.CountryGroup
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeCredentials}
import com.typesafe.config.Config
import data.ContributionData
import models.PaymentMode
import models.PaymentMode.{Default, Testing}
import play.api.mvc.RequestHeader
import utils.TestUserService

import scala.concurrent.ExecutionContext

class PaymentServices(
  config: Config,
  testUserService: TestUserService,
  identityService: IdentityService,
  emailService: EmailService,
  regionalStripeService: RegionalStripeService,
  contributionDataPerMode: Map[PaymentMode, ContributionData],
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) {

  private val paypalServices: Map[PaymentMode, PaypalService] = {
    val paypalExecutionContext = actorSystem.dispatchers.lookup("contexts.paypal-context")
    val paypalConfig = config.getConfig("paypal")
    val supportBackendConfig = config.getConfig("support-backend")
    val supportPaypalExecuteEndpoint = supportBackendConfig.getString("paypal-execute")

    def paypalServiceFor(mode: PaymentMode): PaypalService = {
      val contributionData = contributionDataPerMode(mode)
      val paypalMode = paypalConfig.getString(mode.entryName.toLowerCase)
      val keys = paypalConfig.getConfig(paypalMode)
      val apiConfig = PaypalApiConfig.from(keys, paypalMode)
      new PaypalService(
        config = apiConfig,
        contributionData = contributionData,
        identityService = identityService,
        emailService = emailService,
        supportPaypalExecuteEndpoint = supportPaypalExecuteEndpoint
      )(paypalExecutionContext)
    }

    PaymentMode.values.map(mode => mode -> paypalServiceFor(mode)).toMap
  }

  private def modeFor(displayName: String): PaymentMode =
    if (testUserService.isTestUser(displayName)) Testing else Default

  private def modeFor(request: RequestHeader): PaymentMode =
    if (testUserService.isTestUser(request)) Testing else Default

  def paypalServiceFor(request: RequestHeader): PaypalService = paypalServices(modeFor(request))

  def stripeServiceFor(displayName: String, countryGroup: Option[CountryGroup]): StripeService =
    regionalStripeService.serviceFor(modeFor(displayName), countryGroup)

  def stripeServiceFor(requestHeader: RequestHeader, countryGroup: Option[CountryGroup]): StripeService =
    regionalStripeService.serviceFor(modeFor(requestHeader), countryGroup)

  def stripeServiceFor(mode: PaymentMode, countryGroup: Option[CountryGroup]): StripeService =
    regionalStripeService.serviceFor(mode, countryGroup)

  def stripeKeysFor(requestHeader: RequestHeader): Map[CountryGroup, String] =
    regionalStripeService.regionalServicesFor(modeFor(requestHeader)).mapValues(_.publicKey)
      .withDefaultValue(regionalStripeService.defaultService.publicKey)
}
