package controllers

import java.time.LocalDate

import actions.CommonActions.ABTestRequest
import cats.data.EitherT
import com.gu.i18n._
import com.netaporter.uri.Uri
import com.paypal.api.payments.{Links, Payment}
import controllers.PaypalController.{AuthorizePaymentUtils, ExecutePaymentUtils, UpdateMetaDataUtils}
import controllers.PaypalController.AuthorizePaymentUtils.AuthResponse
import controllers.PaypalController.UpdateMetaDataUtils.MetadataUpdate
import fixtures.TestApplicationFactory
import models.{ContributionAmount, Contributor, IdentityId}
import monitoring.{CloudWatchMetrics, LoggingTags}
import org.mockito.{Matchers, Mockito}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.JsNull
import play.api.mvc._
import play.api.test._
import play.filters.csrf.CSRFCheck
import services.{PaymentServices, PaypalService}

import scala.concurrent.{ExecutionContext, Future}

trait PaypalControllerMocks extends MockitoSugar with EitherValues {
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
    .thenReturn("createTime")

  Mockito.when(mockPaypalPayment.getPayer.getPayerInfo.getEmail)
    .thenReturn("a@b.com")
}

class PaypalControllerSpec extends PlaySpec
  with ScalaFutures
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

  // All the methods in Play's Helper object accept a future of a result, instead of a result.
  // This implicit conversion allows the methods to be applied to a result.
  implicit def asFuture(result: Result): Future[Result] = Future.successful(result)

  val authorizePaymentUtils = new AuthorizePaymentUtils(mockCloudwatchMetrics)

  "The authorize endpoint" when {

    "deciding how much to create the payment" should {

      "cap the amount at 16,000 for Australia" in {

        authorizePaymentUtils.capAmount(16000, AUD) mustEqual 16000
        authorizePaymentUtils.capAmount(16001, AUD) mustEqual 16000
      }

      "cap the amount at 2000 for any other country" in {

        authorizePaymentUtils.capAmount(2000, GBP) mustEqual 2000
        authorizePaymentUtils.capAmount(2001, GBP) mustEqual 2000

        authorizePaymentUtils.capAmount(2000, USD) mustEqual 2000
        authorizePaymentUtils.capAmount(2001, USD) mustEqual 2000
      }
    }

    "getting the auth response from a payment" should {

      "return the auth response if the approval url and payment id is defined" in {

        val links = mock[Links]
        val approvalUrl = "https://www.approvalUrl.com"

        Mockito.when(links.getRel).thenReturn("APPROVAL_URL")
        Mockito.when(links.getHref).thenReturn(approvalUrl)

        val payment = mock[Payment]
        val paymentId = "paymentId"

        Mockito.when(payment.getLinks).thenReturn(java.util.Arrays.asList(links))
        Mockito.when(payment.getId).thenReturn(paymentId)

        val result = authorizePaymentUtils.authResponseFromPayment(payment)

        result.right.value mustEqual AuthResponse(Uri.parse(approvalUrl), paymentId)
      }

      "return an error message otherwise" in {

        val payment = mock[Payment]
        val result = authorizePaymentUtils.authResponseFromPayment(payment)

        result.left.value mustEqual "Unable to parse payment"
      }
    }

    "there is an error authorizing the payment" should {

      "return an internal server error" in {

        implicit val request = FakeRequest()
        val result = authorizePaymentUtils.notOkResult("error message describing what went wrong")

        Helpers.status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }

    "the payment has been authorized successfully" should {

      "return an OK response the body containing auth response serialized as JSON" in {

        implicit val request = FakeRequest()
        val authResponse = AuthResponse(Uri.parse("https://www.approvalUrl.com"), "paymentId")
        val result = authorizePaymentUtils.okResult(authResponse)

        Helpers.status(result) mustEqual OK
        Helpers.contentAsString(result) mustEqual """{"approvalUrl":"https://www.approvalUrl.com","paymentId":"paymentId"}"""
      }
    }
  }

  val executePaymentUtils = new ExecutePaymentUtils(mockPaypalService, CountryGroup.UK, mockCloudwatchMetrics)

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
    // defs as error thrown if they're vals (global crypto instance requires a running application)

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

        // Create time value was mocked earlier
        Helpers.cookies(okAcceptsJsonResult).get(contributionCookieName).map(_.value) must contain("createTime")
        Helpers.cookies(okAcceptsHtmlResult).get(contributionCookieName).map(_.value) must contain("createTime")
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

  val updateMetaDataUtils = new UpdateMetaDataUtils(mockPaypalService)

  "The update meta data endpoint" when {

    "the email is included in the Play session cookie" should {

      "update the marketing opt-in with the boolean value sent in the form" in {

        implicit val request = marketingOptInRequest(optIn = true).withSession("email" -> "a@b.com")

        val contributor: Contributor = mock[Contributor]

        val methodCall = mockPaypalService.updateMarketingOptIn(
          Matchers.anyString, Matchers.anyBoolean, Matchers.any[Option[IdentityId]])(Matchers.any[LoggingTags])

        Mockito.when(methodCall).thenReturn(EitherT.pure[Future, String, Contributor](contributor))

        whenReady(updateMetaDataUtils.updateMarketingOptIn()) { result =>
          result mustEqual true
        }
      }
    }

    "the email is not included in the Play session cookie" should {

      "not update the marketing opt-in" in {

        implicit val request = marketingOptInRequest(optIn = true)

        whenReady(updateMetaDataUtils.updateMarketingOptIn()) { result =>
          result mustEqual false
        }
      }
    }

    "an attempt has been made to update the marketing opt-in" should {

      "redirect to mobile if the contribution amount is available and the request is from Android" in {

        implicit val request = marketingOptInRequest(optIn = true)
          .withSession("platform" -> "android", "amount" -> "10.00USD")

        updateMetaDataUtils.redirectUrl(CountryGroup.UK) mustEqual
          s"x-gu://contribution?date=${LocalDate.now().toString}&amount=10.00USD"
      }

      "redirect to the thank-you page otherwise" in {

        // Value for implicit request argument provided explicitly to prevent multiple implicits being in scope.

        val optInRequest = marketingOptInRequest(optIn = true)

        updateMetaDataUtils.redirectUrl(CountryGroup.UK)(optInRequest) mustEqual "/uk/thank-you"

        updateMetaDataUtils.redirectUrl(CountryGroup.UK) {
          optInRequest.withSession("platform" -> "android")
        } mustEqual "/uk/thank-you"

        updateMetaDataUtils.redirectUrl(CountryGroup.UK) {
          optInRequest.withSession("platform" -> "ios", "amount" -> "10.00USD")
        } mustEqual "/uk/thank-you"
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

  def marketingOptInRequest(optIn: Boolean): FakeRequest[MetadataUpdate] =
    FakeRequest().withBody(MetadataUpdate(optIn))
}
