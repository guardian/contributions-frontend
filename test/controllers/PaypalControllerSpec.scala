package controllers

import cats.data.EitherT
import com.gu.i18n.{CountryGroup, GBP}
import com.paypal.api.payments.Payment
import configuration.{CorsConfig, SupportConfig}
import fixtures.TestApplicationFactory
import models.ContributionAmount
import monitoring.{CloudWatchMetrics, LoggingTags}
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.{HeaderNames, Status}
import play.api.mvc._
import play.api.test._
import play.filters.csrf.CSRFCheck
import services.{PaymentServices, PaypalService}

import scala.concurrent.{ExecutionContext, Future}

trait PaypalControllerMocks extends MockitoSugar {
  val mockPaymentServices: PaymentServices = mock[PaymentServices]

  val mockPaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  val mockCsrfCheck: CSRFCheck = mock[CSRFCheck]

  val mockCloudwatchMetrics = mock[CloudWatchMetrics]

  val mockCorsConfig = mock[CorsConfig]

  val supportConfig = SupportConfig("https://support.thegulocal.com/thankyou")

  Mockito.when(mockPaymentServices.paypalServiceFor(Matchers.any[Request[_]]))
    .thenReturn(mockPaypalService)

  Mockito.when(mockPaypalService.paymentAmount(mockPaypalPayment))
    .thenReturn(Some(mockContributionAmount))

  Mockito.when(mockPaypalPayment.getCreateTime)
    .thenReturn("")

  Mockito.when(mockPaypalPayment.getPayer.getPayerInfo.getEmail)
    .thenReturn("a@b.com")
}

class PaypalControllerSpec extends PlaySpec
  with PaypalControllerMocks
  with HeaderNames
  with Status
  with ResultExtractors
  with DefaultAwaitTimeout
  with TestApplicationFactory
  with BaseOneAppPerSuite {

  import cats.instances.future._

  implicit val executionContext: ExecutionContext = app.actorSystem.dispatcher

  val controller: PaypalController = new PaypalController(mockPaymentServices, mockCorsConfig, supportConfig, mockCsrfCheck, mockCloudwatchMetrics)
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/").withHeaders(("Accept", "text/html"))

  def executePayment: Action[AnyContent] = controller.executePayment(
    countryGroup = CountryGroup.UK,
    paymentId = "test",
    token = "test",
    payerId = "test",
    cmp = None,
    intCmp = None,
    refererPageviewId = None,
    refererUrl = None,
    ophanBrowserId = None,
    ophanPageviewId = None,
    ophanVisitId = None,
    supportRedirect = None
  )

  def executeSupportPayment: Action[AnyContent] = controller.executePayment(
    countryGroup = CountryGroup.UK,
    paymentId = "test",
    token = "test",
    payerId = "test",
    cmp = None,
    intCmp = None,
    refererPageviewId = None,
    refererUrl = None,
    ophanBrowserId = None,
    ophanPageviewId = None,
    ophanVisitId = None,
    supportRedirect = Some(true)
  )

  "Paypal Controller" should {

    "generate correct redirect URL for support's successful PayPal payments" in {
      Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
        .thenReturn(EitherT.right[Future, String, Payment](Future.successful(mockPaypalPayment)))

      val result: Future[Result] = executeSupportPayment(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("https://support.thegulocal.com/thankyou"))
    }

    "generate correct redirect URL for successful PayPal payments" in {
      Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
        .thenReturn(EitherT.right[Future, String, Payment](Future.successful(mockPaypalPayment)))

      val result: Future[Result] = executePayment(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk/post-payment"))
    }

    "generate correct redirect URL for unsuccessful PayPal payments" in {
      Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
        .thenReturn(EitherT.left[Future, String, Payment](Future.successful("Error")))

      val result: Future[Result] = executePayment(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk?error_code=PaypalError"))
    }
  }
}
