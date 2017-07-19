import cats.data.EitherT
import com.gu.i18n.GBP
import com.paypal.api.payments.Payment
import models.ContributionAmount
import monitoring.{CloudWatchMetrics, LoggingTags}
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.FakeApplicationFactory
import play.api._
import play.api.mvc.Request
import play.core.DefaultWebCommands
import services.{EmailService, PaymentServices, PaypalService}
import wiring.AppComponents

import scala.concurrent.{ExecutionContext, Future}

trait TestComponents extends MockitoSugar {
  self: AppComponents =>
  import cats.instances.future._

  val mockPaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  override lazy val emailService: EmailService = mock[EmailService]
  override val jdbcExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  override lazy val cloudWatchMetrics: CloudWatchMetrics = mock[CloudWatchMetrics]
  override lazy val paymentServices: PaymentServices = mock[PaymentServices]

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
