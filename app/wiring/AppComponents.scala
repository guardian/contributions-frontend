package wiring

import com.github.nscala_time.time.Imports._
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.identity.play.AccessCredentials.{Cookies, Token}
import com.gu.identity.testing.usernames.TestUsernames
import com.gu.monitoring.ServiceMetrics
import com.gu.okhttp.RequestRunners
import com.softwaremill.macwire._
import com.typesafe.config.ConfigFactory
import controllers._
import data.ContributionData
import filters.CheckCacheHeadersFilter
import models.PaymentMode
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.gzip.GzipFilterComponents
import play.filters.headers.{SecurityHeadersConfig, SecurityHeadersFilter}
import services.{EmailService, IdentityService, OphanService, PaymentServices}
import router.Routes
import configuration.{Config, CorsConfig, SupportConfig}
import monitoring.{CloudWatch, CloudWatchMetrics}

//Sometimes intellij deletes this -> (import router.Routes)

/* https://www.playframework.com/documentation/2.5.x/ScalaCompileTimeDependencyInjection
 * https://github.com/adamw/macwire/blob/master/README.md#play24x
 */
trait AppComponents extends PlayComponents with GzipFilterComponents {

  lazy val config = ConfigFactory.load()
  lazy val stripeConfig = config.getConfig("stripe")
  lazy val corsConfig = CorsConfig.from(config.getConfig("cors"))
  lazy val supportConfig = SupportConfig.from(config.getConfig("support-frontend"))
  private val idConfig = config.getConfig("identity")

  lazy val identityKeys = if (idConfig.getBoolean("production.keys")) new ProductionKeys else new PreProductionKeys

  lazy val testUsernames = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(idConfig.getString("test.users.secret")),
    recency = 2.days.standardDuration
  )
  lazy val identityAuthProvider =
    Cookies.authProvider(identityKeys).withDisplayNameProvider(Token.authProvider(identityKeys, "membership"))

  val contributionDataPerMode: Map[PaymentMode, ContributionData] = {
    val dbConfig = config.getConfig("dbConf")
    def contributionDataFor(mode: PaymentMode) = {
      val modeKey = dbConfig.getString(mode.entryName.toLowerCase)
      new ContributionData(dbApi.database(modeKey))(jdbcExecutionContext) // explicit execution context to avoid blocking the app
    }
    PaymentMode.values.map(mode => mode -> contributionDataFor(mode)).toMap
  }

  lazy val cloudWatchClient = CloudWatch.build()
  lazy val cloudWatchMetrics = new CloudWatchMetrics(cloudWatchClient)

  lazy val paymentServices = new PaymentServices(
    config = config,
    authProvider = identityAuthProvider,
    testUsernames = testUsernames,
    identityService = identityService,
    emailService = emailService,
    contributionDataPerMode = contributionDataPerMode,
    actorSystem = actorSystem
  )

  val ophanMetrics: ServiceMetrics = new ServiceMetrics(Config.stage, "ophan", "tracker")

  lazy val identityService = new IdentityService(wsClient, idConfig)
  lazy val emailService = wire[EmailService]

  lazy val ophanService = new OphanService(RequestRunners.loggingRunner(ophanMetrics), environment)
  lazy val giraffeController = wire[Contributions]
  lazy val healthcheckController = wire[Healthcheck]
  lazy val assetController = wire[Assets]
  lazy val paypalController = wire[PaypalController]
  lazy val stripeController = new StripeController(paymentServices, stripeConfig, corsConfig, ophanService, cloudWatchMetrics)
  lazy val userController = wire[UserController]
  lazy val epicComponentsController = wire[EpicComponentsController]

  override lazy val httpErrorHandler =
    new monitoring.ErrorHandler(identityAuthProvider, environment, configuration, sourceMapper, Some(router))

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    gzipFilter,
    wire[CheckCacheHeadersFilter],
    SecurityHeadersFilter(SecurityHeadersConfig(
      contentSecurityPolicy = None,
      frameOptions = Some("ALLOW-FROM https://www.theguardian.com/")
    ))
  )

  val prefix: String = "/"
  lazy val router: Router = wire[Routes]
}
