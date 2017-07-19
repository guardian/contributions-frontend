import cats.data.EitherT
import com.gu.i18n.GBP
import com.gu.identity.cookie.IdentityKeys
import com.gu.identity.play.AuthenticatedIdUser.Provider
import com.gu.monitoring.ServiceMetrics
import com.paypal.api.payments.Payment
import com.typesafe.config.Config
import models.{ContributionAmount, PaymentMode}
import monitoring.{CloudWatchMetrics, LoggingTags}
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.FakeApplicationFactory
import data.ContributionData
import play.api._
import play.api.mvc.Request
import play.core.DefaultWebCommands
import services.{EmailService, IdentityService, PaymentServices, PaypalService}
import wiring.AppComponents

import scala.concurrent.{ExecutionContext, Future}

trait TestComponents extends MockitoSugar {
  self: AppComponents =>
  import cats.instances.future._

  val mockPaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  override lazy val config: Config = mock[Config]

  override lazy val contributionDataPerMode: Map[PaymentMode, ContributionData] = Map(
    PaymentMode.Testing -> mock[ContributionData],
    PaymentMode.Default -> mock[ContributionData]
  )

  override lazy val identityService: IdentityService = mock[IdentityService]
  override lazy val identityKeys: IdentityKeys = mock[IdentityKeys]
  override lazy val identityAuthProvider: Provider = mock[Provider]

  override lazy val ophanMetrics: ServiceMetrics = mock[ServiceMetrics]

  override lazy val emailService: EmailService = mock[EmailService]
  override val jdbcExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  override lazy val cloudWatchMetrics: CloudWatchMetrics = mock[CloudWatchMetrics]
  override lazy val paymentServices: PaymentServices = mock[PaymentServices]

  Mockito.when(config.getConfig(Matchers.anyString))
    .thenReturn(config)

  Mockito.when(config.getString(Matchers.anyString))
    .thenReturn("")

  Mockito.when(paymentServices.paypalServiceFor(Matchers.any[Request[_]]))
    .thenReturn(mockPaypalService)

  Mockito.when(mockPaypalService.paymentAmount(mockPaypalPayment))
    .thenReturn(Some(mockContributionAmount))

  Mockito.when(mockPaypalPayment.getCreateTime)
    .thenReturn("")

  Mockito.when(mockPaypalPayment.getPayer.getPayerInfo.getEmail)
    .thenReturn("a@b.com")

  Mockito.when(mockPaypalService.executePayment(Matchers.eq("PASS"), Matchers.eq("PASS"))(Matchers.any[LoggingTags]))
    .thenReturn(EitherT.right[Future, String, Payment](Future.successful(mockPaypalPayment)))

  Mockito.when(mockPaypalService.executePayment(Matchers.eq("FAIL"), Matchers.eq("FAIL"))(Matchers.any[LoggingTags]))
    .thenReturn(EitherT.left[Future, String, Payment](Future.successful("failed")))
}

trait TestApplicationFactory extends FakeApplicationFactory {

  class TestApplicationBuilder {

    def build(): Application = {
      val env = Environment.simple()
      val context = ApplicationLoader.Context(
        environment = env,
        sourceMapper = None,
        webCommands = new DefaultWebCommands(),
        initialConfiguration = Configuration.load(env)
      )

      (new BuiltInComponentsFromContext(context) with AppComponents with TestComponents).application
    }
  }

  override def fakeApplication(): Application =
    new TestApplicationBuilder().build()
}
