package services

import com.gu.i18n.CountryGroup
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeCredentials}
import com.typesafe.config.Config
import data.ContributionData
import models.PaymentMode

import scala.concurrent.ExecutionContext

trait RegionalStripeService {

  def defaultService: StripeService

  def regionalServicesFor(mode: PaymentMode): Map[CountryGroup, StripeService]

  def serviceFor(mode: PaymentMode, countryGroup: Option[CountryGroup]): StripeService

}

class DefaultRegionalStripeService(config: Config,
                                   contributionDataPerMode: Map[PaymentMode, ContributionData],
                                   identityService: IdentityService,
                                   emailService: EmailService)(implicit ec: ExecutionContext)
  extends RegionalStripeService {

  import utils.ConfigUtils._

  private val stripeConfig = config.getConfig("stripe")

  // map country groups to configs (only in instances where there is a config for the country group's ID)
  private val countryGroupConfigs: Map[CountryGroup, Config] = {
    CountryGroup.allGroups.map { group =>
      group -> stripeConfig.getOptionalConfig(s"keys.${group.id.toLowerCase}")
    }.collect { case (group, Some(config)) => group -> config }.toMap
  }

  private def modeName(mode: PaymentMode): String =
    stripeConfig.getString(mode.entryName.toLowerCase)

  private def credentialsFor(mode: PaymentMode, config: Config): StripeCredentials = {
    val keys: Config = config.getConfig(modeName(mode))

    StripeCredentials(
      secretKey = keys.getString("secret"),
      publicKey = keys.getString("public")
    )
  }

  private def stripeServiceFor(countryGroup: CountryGroup, mode: PaymentMode): StripeService = {
    val config: Config = countryGroupConfigs.get(countryGroup).getOrElse(stripeConfig.getConfig(s"keys.default"))
    val stripeMode: String = modeName(mode)
    val credentials: StripeCredentials = credentialsFor(mode, config)

    val metrics = new ServiceMetrics(stripeMode, "giraffe", "stripe")

    new StripeService(
      apiConfig = StripeApiConfig(stripeMode, credentials),
      metrics = metrics,
      contributionData = contributionDataPerMode(mode),
      identityService = identityService,
      emailService = emailService
    )
  }

  private val stripeServices: Map[PaymentMode, Map[CountryGroup, StripeService]] = {
    PaymentMode.values.map { mode =>
      mode -> CountryGroup.allGroups.map(group => group -> stripeServiceFor(group, mode)).toMap
    }.toMap
  }

  override def defaultService: StripeService =
    regionalServicesFor(PaymentMode.Default)(CountryGroup.UK)

  override def regionalServicesFor(mode: PaymentMode) =
    stripeServices(mode)

  override def serviceFor(mode: PaymentMode, countryGroup: Option[CountryGroup]): StripeService =
    countryGroup.map(group => regionalServicesFor(mode)(group)).getOrElse(defaultService)
}
