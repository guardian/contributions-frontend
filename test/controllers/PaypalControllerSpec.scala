import cats.data.EitherT
import com.gu.i18n.{CountryGroup, GBP}
import com.paypal.api.payments.Payment
import controllers.PaypalController
import models.ContributionAmount
import monitoring.{CloudWatchMetrics, LoggingTags}
import org.mockito.{Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import play.filters.csrf.CSRFCheck
import services.{PaymentServices, PaypalService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaypalControllerSpec extends PlaySpec
  with Results
  with MockitoSugar
  with BaseOneAppPerTest
  with TestApplicationFactory {

  import cats.instances.future._

  val mockCSRFCheck: CSRFCheck = mock[CSRFCheck]
  val mockCloudWatchMetrics: CloudWatchMetrics = mock[CloudWatchMetrics]
  val mockPaymentServices: PaymentServices = mock[PaymentServices]
  val mockPaypalService: PaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockLoggingTags: LoggingTags = mock[LoggingTags]
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  val fakeHeaders: FakeHeaders = FakeHeaders(Seq(("Accept", "text/html")))
  val fakeRequest: Request[AnyContent] = FakeRequest("GET", "/", fakeHeaders, AnyContentAsEmpty)

  Mockito.when(mockPaymentServices.paypalServiceFor(Matchers.any[Request[_]]))
    .thenReturn(mockPaypalService)

  Mockito.when(mockPaypalPayment.getCreateTime)
    .thenReturn("")

  Mockito.when(mockPaypalService.paymentAmount(mockPaypalPayment))
    .thenReturn(Some(mockContributionAmount))

  Mockito.when(mockPaypalPayment.getPayer.getPayerInfo.getEmail)
    .thenReturn("a@b.com")

  WsTestClient.withClient { client =>

    val controller = new PaypalController(client, mockPaymentServices, mockCSRFCheck, mockCloudWatchMetrics)
    val executePayment = controller.executePayment(
      countryGroup = CountryGroup.UK,
      paymentId = "1",
      token = "hello",
      payerId = "YO",
      cmp = None,
      intCmp = None,
      refererPageviewId = None,
      refererUrl = None,
      ophanPageviewId = None,
      ophanBrowserId = None,
      ophanVisitId = None
    )

    "Paypal Controller" should {

      "generate correct redirect URL for successful PayPal payments" in {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.right[Future, String, Payment](Future.successful(mockPaypalPayment)))

        val result = executePayment.apply(fakeRequest)

        status(result).mustBe(303)
        redirectLocation(result).mustBe(Some("/uk/post-payment"))
      }

      "generate correct redirect URL for unsuccessful PayPal payments" in {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, String, Payment](Future.successful("failed")))

        val result = executePayment.apply(fakeRequest)

        status(result).mustBe(303)
        redirectLocation(result).mustBe(Some("/uk?error_code=PaypalError"))
      }
    }

  }
}
