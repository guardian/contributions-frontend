package controllers

import cats.data.EitherT
import com.gu.i18n.{CountryGroup, GBP}
import com.paypal.api.payments.Payment
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

import scala.concurrent.Future

trait PaypalControllerMocks extends MockitoSugar {
  val mockPaymentServices: PaymentServices = mock[PaymentServices]

  val mockPaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  val mockCsrfCheck: CSRFCheck = mock[CSRFCheck]

  val mockCloudwatchMetrics = mock[CloudWatchMetrics]

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

  import scala.concurrent.ExecutionContext.Implicits.global
  import cats.instances.future._

  val controller: PaypalController = new PaypalController(mockPaymentServices, mockCsrfCheck, mockCloudwatchMetrics)
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
    ophanVisitId = None
  )

  "Paypal Controller" should {

    "generate correct redirect URL for successful PayPal payments" in {
      Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
        .thenReturn(EitherT.right[Future, String, Payment](Future.successful(mockPaypalPayment)))

      val result: Future[Result] = executePayment.apply(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk/post-payment"))
    }

    "generate correct redirect URL for unsuccessful PayPal payments" in {
      Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
        .thenReturn(EitherT.left[Future, String, Payment](Future.successful("Error")))

      val result: Future[Result] = executePayment.apply(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk?error_code=PaypalError"))
    }
  }
}
