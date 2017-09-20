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
  contributionDataPerMode: Map[PaymentMode, ContributionData],
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) {

  import utils.ConfigUtils._
  import utils.FastlyUtils._

  def stripeServices(maybeCountryGroup: Option[CountryGroup]): Map[PaymentMode, StripeService] = {

    val stripeConfig = config.getConfig("stripe")

    def stripeServiceFor(mode: PaymentMode): StripeService = {
      val contributionData = contributionDataPerMode(mode)
      val stripeMode = stripeConfig.getString(mode.entryName.toLowerCase)

      val keys = maybeCountryGroup.flatMap(cg => stripeConfig.getOptionalConfig(s"keys.${cg.id.toLowerCase}"))
        .getOrElse(stripeConfig.getConfig(s"keys.default"))
        .getConfig(stripeMode)

      val credentials = StripeCredentials(
        secretKey = keys.getString("secret"),
        publicKey = keys.getString("public")
      )

      val metrics = new ServiceMetrics(stripeMode, "giraffe", "stripe")

      new StripeService(
        apiConfig = StripeApiConfig(stripeMode, credentials),
        metrics = metrics,
        contributionData = contributionData,
        identityService = identityService,
        emailService = emailService
      )
    }

    PaymentMode.values.map(mode => mode -> stripeServiceFor(mode)).toMap
  }

  val paypalServices: Map[PaymentMode, PaypalService] = {
    val paypalExecutionContext = actorSystem.dispatchers.lookup("contexts.paypal-context")
    val paypalConfig = config.getConfig("paypal")

    def paypalServiceFor(mode: PaymentMode): PaypalService = {
      val contributionData = contributionDataPerMode(mode)
      val paypalMode = paypalConfig.getString(mode.entryName.toLowerCase)
      val keys = paypalConfig.getConfig(paypalMode)
      val apiConfig = PaypalApiConfig.from(keys, paypalMode)
      new PaypalService(
        config = apiConfig,
        contributionData = contributionData,
        identityService = identityService,
        emailService = emailService
      )(paypalExecutionContext)
    }

    PaymentMode.values.map(mode => mode -> paypalServiceFor(mode)).toMap
  }

  private def modeFor(displayName: String): PaymentMode =
    if (testUserService.isTestUser(displayName)) Testing else Default

  private def modeFor(request: RequestHeader): PaymentMode =
    if (testUserService.isTestUser(request)) Testing else Default

  def stripeServiceFor(displayName: String, request: RequestHeader): StripeService =
    stripeServices(request.getFastlyCountryGroup)(modeFor(displayName))

  def stripeServiceFor(request: RequestHeader): StripeService =
    stripeServices(request.getFastlyCountryGroup)(modeFor(request))


  def paypalServiceFor(request: RequestHeader): PaypalService = paypalServices(modeFor(request))

}
