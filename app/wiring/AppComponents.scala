package wiring

import com.github.nscala_time.time.Imports._
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.identity.play.AccessCredentials.{Cookies, Token}
import com.gu.identity.testing.usernames.TestUsernames
import com.softwaremill.macwire._
import com.typesafe.config.ConfigFactory
import controllers._
import data.ContributionData
import filters.CheckCacheHeadersFilter
import play.api.libs.crypto.CSRFTokenSigner
import models.PaymentMode
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.filters.gzip.GzipFilterComponents
import play.filters.csrf.CSRFAddToken
import play.filters.csrf.CSRFCheck
import play.filters.headers.{SecurityHeadersConfig, SecurityHeadersFilter}
import services.{IdentityService, PaymentServices}

import router.Routes

//Sometimes intellij deletes this -> (import router.Routes)

/* https://www.playframework.com/documentation/2.5.x/ScalaCompileTimeDependencyInjection
 * https://github.com/adamw/macwire/blob/master/README.md#play24x
 */
trait AppComponents extends PlayComponents with GzipFilterComponents {

  lazy val config = ConfigFactory.load()
  lazy val stripeConfig = config.getConfig("stripe")
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
      val modeKey = dbConfig.getString(mode.name)
      new ContributionData(dbApi.database(modeKey))(jdbcExecutionContext) // explicit execution context to avoid blocking the app
    }
    PaymentMode.all.map(mode => mode -> contributionDataFor(mode)).toMap
  }

  lazy val paymentServices = new PaymentServices(
    config = config,
    authProvider = identityAuthProvider,
    testUsernames = testUsernames,
    identityService = identityService,
    contributionDataPerMode = contributionDataPerMode,
    actorSystem = actorSystem
  )
  lazy val identityService = new IdentityService(wsClient, idConfig)

  lazy val giraffeController = wire[Giraffe]
  lazy val healthcheckController = wire[Healthcheck]
  lazy val assetController = wire[Assets]
  lazy val paypalController = wire[PaypalController]
  lazy val stripeController = new StripeController(paymentServices, stripeConfig)

  // The in-scope variables crypto and csrfTokenSigner are both CSRFTokenSigner's,
  // therefore, create a method which can be used to specify which one wire should use.
  // Note: this will be unnecessary when crypto is removed in a future version of Play.
  def getCSRFAddToken(crypto: CSRFTokenSigner): CSRFAddToken = wire[CSRFAddToken]
  def getCSRFCheck(crypto: CSRFTokenSigner): CSRFCheck = wire[CSRFCheck]

  lazy val addToken = getCSRFAddToken(csrfTokenSigner)
  lazy val check = getCSRFCheck(crypto)

  override lazy val httpErrorHandler =
    new monitoring.ErrorHandler(identityAuthProvider, environment, configuration, sourceMapper, Some(router))

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    gzipFilter,
    wire[CheckCacheHeadersFilter],
    SecurityHeadersFilter(SecurityHeadersConfig(
      contentSecurityPolicy = None,
      frameOptions = Some("SAMEORIGIN")
    ))
  )

  val prefix: String = "/"
  lazy val router: Router = wire[Routes]
}
