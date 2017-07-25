package controllers

import actions.CommonActions.ABTestRequest
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
import play.api.libs.json.JsNull
import play.api.mvc._
import play.api.test._
import play.filters.csrf.CSRFCheck
import services.{PaymentServices, PaypalService}

import scala.concurrent.{ExecutionContext, Future}

trait PaypalControllerMocks extends MockitoSugar {
  val mockPaymentServices: PaymentServices = mock[PaymentServices]

  val mockPaypalService: PaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  val mockCsrfCheck: CSRFCheck = mock[CSRFCheck]

  val mockCloudwatchMetrics: CloudWatchMetrics = mock[CloudWatchMetrics]

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

  import PaypalControllerSpec._
  import cats.instances.future._

  implicit val executionContext: ExecutionContext = app.actorSystem.dispatcher

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

  val executePaymentUtils = new PaypalController.ExecutePaymentUtils(
    mockPaypalService, CountryGroup.UK, mockCloudwatchMetrics)

  // All the methods in Play's Helper object accept a future of a result, instead of a result.
  // This implicit conversion allows the methods to be applied to a result.
  implicit def asFuture[A](a: A): Future[A] = Future.successful(a)

  "The execute endpoint" when {

    "there is an error executing a Paypal payment" should {

      "return a bad request with an empty JSON body if the request accepts JSON" in {

        implicit val abTestRequest = abTestRequestAcceptingJson
        val result = executePaymentUtils.notOkResult("error message describing what went wrong")

        result.header.status mustEqual BAD_REQUEST
        Helpers.contentAsJson(result) mustEqual JsNull
      }

      "redirect the client to an error page if the request accepts HTML" in {

        implicit val abTestRequest = abTestRequestAcceptingHtml
        val result = executePaymentUtils.notOkResult("error message describing what went wrong")

        Helpers.status(result) mustEqual SEE_OTHER
        Helpers.header("Location", result) must contain(s"/uk?error_code=PaypalError")
      }
    }

    // These results are factored out as they're used in multiple places.
    // Error thrown if they're vals (global crypto instance requires a running application)

    def okAcceptsJsonResult = {
      implicit val abTestRequest = abTestRequestAcceptingJson
      executePaymentUtils.okResult(mockPaypalPayment)
    }

    def okAcceptsHtmlResult = {
      implicit val abTestRequest = abTestRequestAcceptingHtml
      executePaymentUtils.okResult(mockPaypalPayment)
    }

    "a Paypal payment is executed successfully" should {

      "return an OK request with an empty body if the request accepts JSON" in {

        Helpers.status(okAcceptsJsonResult) mustEqual OK
        Helpers.contentAsJson(okAcceptsJsonResult) mustEqual JsNull
      }

      "redirect the client to /uk/post-payment if the request accepts HTML" in {

        Helpers.status(okAcceptsHtmlResult) mustEqual SEE_OTHER
        Helpers.header("Location", okAcceptsHtmlResult) must contain("/uk/post-payment")
      }

      "set a cookie with value equal to the time the user contributed" in {

        val contributionCookieName = "gu.contributions.contrib-timestamp"
        Helpers.cookies(okAcceptsJsonResult).get(contributionCookieName) mustBe defined
        Helpers.cookies(okAcceptsHtmlResult).get(contributionCookieName) mustBe defined
      }
    }

    "the client is redirected after a successful Paypal payment" should {

      "include the email in the Play session cookie" in {

        // Email address mocked earlier on
        Helpers.session(okAcceptsHtmlResult).get("email") must contain("a@b.com")
      }

      "include the amount in the Play session cookie" in {

        // Amount mocked earlier on
        Helpers.session(okAcceptsHtmlResult).get("amount") must contain("10.00GBP")
      }
    }
  }
}

object PaypalControllerSpec {

  def abTestRequestWithAccepts(accepts: String): ABTestRequest[AnyContent] = {
    val request = FakeRequest().withHeaders("Accept" -> accepts).withBody[AnyContent](AnyContentAsEmpty)
    ABTestRequest(testId = 1, request)
  }

  val abTestRequestAcceptingJson: ABTestRequest[AnyContent] = abTestRequestWithAccepts("application/json")

  val abTestRequestAcceptingHtml: ABTestRequest[AnyContent] = abTestRequestWithAccepts("text/html")
}
