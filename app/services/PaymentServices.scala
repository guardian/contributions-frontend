package services

import abtests.Test.HumaniseTestV2
import actions.CommonActions.ABTestRequest
import akka.actor.ActorSystem
import com.gu.identity.play.AuthenticatedIdUser
import com.gu.identity.testing.usernames.TestUsernames
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeCredentials}
import com.typesafe.config.Config
import data.ContributionData
import models.PaymentMode
import models.PaymentMode.{Default, Testing}
import play.api.mvc.AnyContent
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PaymentServices(
  config: Config,
  authProvider: AuthenticatedIdUser.Provider,
  testUsernames: TestUsernames,
  identityService: IdentityService,
  emailService: EmailService,
  contributionDataPerMode: Map[PaymentMode, ContributionData],
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) {

  val stripeServices: Map[PaymentMode, StripeService] = {
    val stripeConfig = config.getConfig("stripe")

    def stripeServiceFor(mode: PaymentMode): StripeService = {
      val contributionData = contributionDataPerMode(mode)
      val stripeMode = stripeConfig.getString(mode.entryName.toLowerCase)
      val keys = stripeConfig.getConfig(s"keys.$stripeMode")
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

  private def isTestUser(request: RequestHeader): Boolean = authProvider(request).flatMap(_.displayName).exists(testUsernames.isValid)

  private def modeFor(request: RequestHeader): PaymentMode = if (isTestUser(request)) Testing else Default

  def stripeServiceFor(request: RequestHeader): StripeService = stripeServices(modeFor(request))

  def paypalServiceFor(request: RequestHeader): PaypalService = paypalServices(modeFor(request))

  def getHumaniseTestV2VariantData(request: ABTestRequest[AnyContent]): Future[HumaniseTestV2.VariantData] =
    contributionDataPerMode(modeFor(request)).getHumaniseTestV2VariantData(request)
}
