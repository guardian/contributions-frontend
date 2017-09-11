package controllers

import abtests.Allocation
import akka.stream.Materializer
import cats.data.EitherT
import com.gu.i18n.{CountryGroup, GBP}
import com.paypal.api.payments.{Capture, Payment}
import configuration.{CorsConfig, SupportConfig}
import fixtures.TestApplicationFactory
import models.{ContributionAmount, IdentityId, PaypalApiError, SavedContributionData}
import monitoring.{CloudWatchMetrics, LoggingTags}
import org.mockito.{ArgumentCaptor, Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.{HeaderNames, Status}
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.filters.csrf.CSRFCheck
import services.{ContributionOphanService, PaymentServices, PaypalService}
import cats.instances.future._
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import services.ContributionOphanService.AcquisitionSubmissionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class PaypalControllerFixture(implicit ec: ExecutionContext) extends MockitoSugar {

  val mockPaymentServices: PaymentServices = mock[PaymentServices]

  val mockPaypalService: PaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  val mockCsrfCheck: CSRFCheck = mock[CSRFCheck]

  val mockCloudwatchMetrics: CloudWatchMetrics = mock[CloudWatchMetrics]

  val mockCorsConfig = mock[CorsConfig]

  val mockOphanService = mock[ContributionOphanService]

  val supportConfig = SupportConfig("https://support.thegulocal.com/thankyou")

  Mockito.when(mockPaymentServices.paypalServiceFor(Matchers.any[Request[_]]))
    .thenReturn(mockPaypalService)

  Mockito.when(mockPaypalService.paymentAmount(mockPaypalPayment))
    .thenReturn(Some(mockContributionAmount))

  Mockito.when(mockPaypalPayment.getCreateTime)
    .thenReturn("")

  Mockito.when(mockPaypalPayment.getPayer.getPayerInfo.getEmail)
    .thenReturn("a@b.com")

  Mockito.when(mockPaypalService.getPayment(Matchers.anyString)(Matchers.any[LoggingTags]))
    .thenReturn(EitherT.pure[Future, PaypalApiError, Payment](mockPaypalPayment))

  Mockito.when(mockPaypalService.storeMetaData(
    payment = Matchers.any[Payment],
    testAllocations = Matchers.any[Set[Allocation]],
    cmp = Matchers.any[Option[String]],
    intCmp = Matchers.any[Option[String]],
    refererPageviewId = Matchers.any[Option[String]],
    refererUrl = Matchers.any[Option[String]],
    ophanPageviewId = Matchers.any[Option[String]],
    ophanBrowserId = Matchers.any[Option[String]],
    idUser = Matchers.any[Option[IdentityId]],
    platform = Matchers.any[Option[String]],
    ophanVisitId = Matchers.any[Option[String]]
  )(Matchers.any[LoggingTags]))
    .thenReturn(EitherT.pure[Future, String, SavedContributionData](mock[SavedContributionData]))

  val controller: PaypalController = new PaypalController(mockPaymentServices, mockCorsConfig, supportConfig, mockCsrfCheck, mockCloudwatchMetrics, mockOphanService)

  def numberOfCallsToStoreMetaDataMustBe(times: Int): Unit = {
    def captor[A <: AnyRef](implicit classTag: ClassTag[A]): A =
      ArgumentCaptor.forClass[A](classTag.runtimeClass.asInstanceOf[Class[A]]).capture()

    Mockito.verify(mockPaypalService, VerificationModeFactory.times(times)).storeMetaData(
      captor[Payment],
      captor[Set[Allocation]],
      captor[Option[String]],
      captor[Option[String]],
      captor[Option[String]],
      captor[Option[String]],
      captor[Option[String]],
      captor[Option[String]],
      captor[Option[IdentityId]],
      captor[Option[String]],
      captor[Option[String]]
    )(captor[LoggingTags])
  }

  def numberOfAcquisitionEventSubmissionsShouldBe(times: Int): Unit =
    Mockito.verify(mockOphanService, VerificationModeFactory.times(times))
      .submitAcquisition[Any](Matchers.any[Any])(
        Matchers.any[AcquisitionSubmissionBuilder[Any]],
        Matchers.any[ExecutionContext]
      )
}

class PaypalControllerSpec extends PlaySpec
  with HeaderNames
  with Status
  with ResultExtractors
  with DefaultAwaitTimeout
  with TestApplicationFactory
  with BaseOneAppPerSuite
  with ScalaFutures {

  implicit val executionContext: ExecutionContext = app.actorSystem.dispatcher
  implicit val mat: Materializer = app.materializer

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/").withHeaders(("Accept", "text/html"))

  def executePayment(controller: PaypalController): Action[AnyContent] = controller.executePayment(
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
    componentId = None,
    componentType = None,
    source = None,
    abTest = None,
    supportRedirect = None
  )

  def executeSupportPayment(controller: PaypalController): Action[AnyContent] = controller.executePayment(
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
    componentId = None,
    componentType = None,
    source = None,
    abTest = None,
    supportRedirect = Some(true)
  )

  "Paypal Controller" should {

    "generate correct redirect URL for support's successful PayPal payments" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.right[Future, PaypalApiError, Payment](Future.successful(mockPaypalPayment)))
      }

      val result: Future[Result] = executeSupportPayment(fixture.controller)(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("https://support.thegulocal.com/thankyou"))
    }

    "generate correct redirect URL for successful PayPal payments" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.pure[Future, PaypalApiError, Payment](mockPaypalPayment))
      }

      val result: Future[Result] = executePayment(fixture.controller)(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk/post-payment"))

      fixture.numberOfCallsToStoreMetaDataMustBe(1)
    }

    "generate correct redirect URL for unsuccessful PayPal payments" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, PaypalApiError, Payment](Future.successful(PaypalApiError.fromString("Error"))))
      }

      val result: Future[Result] = executePayment(fixture.controller)(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk?error_code=PaypalError"))

      fixture.numberOfCallsToStoreMetaDataMustBe(0)
    }

    val captureRequest = FakeRequest("POST", "/paypal/capture").withJsonBody(Json.parse(
      """{
            "paymentId": "PAY_u27dioc90iojdew",
            "platform": "android",
            "idUser": "abc123",
            "intCmp": "SOME_CMP_CODE"
           }
        """.stripMargin))

    "capture a payment made on mobile" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.capturePayment(Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.pure[Future, PaypalApiError, Capture](mock[Capture]))
      }

      val result: Future[Result] = Helpers.call(fixture.controller.capturePayment, captureRequest)
      status(result).mustBe(200)

      fixture.numberOfCallsToStoreMetaDataMustBe(1)
    }

    "capture a payment even if storing the metadata failed" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.capturePayment(Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.pure[Future, PaypalApiError, Capture](mock[Capture]))

        Mockito.when(mockPaypalService.storeMetaData(
          payment = Matchers.any[Payment],
          testAllocations = Matchers.any[Set[Allocation]],
          cmp = Matchers.any[Option[String]],
          intCmp = Matchers.any[Option[String]],
          refererPageviewId = Matchers.any[Option[String]],
          refererUrl = Matchers.any[Option[String]],
          ophanPageviewId = Matchers.any[Option[String]],
          ophanBrowserId = Matchers.any[Option[String]],
          idUser = Matchers.any[Option[IdentityId]],
          platform = Matchers.any[Option[String]],
          ophanVisitId = Matchers.any[Option[String]]
        )(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, String, SavedContributionData](Future.successful("Error")))
      }



      val result: Future[Result] = Helpers.call(fixture.controller.capturePayment, captureRequest)
      status(result).mustBe(200)

      fixture.numberOfCallsToStoreMetaDataMustBe(1)
    }

    "submit an acquisition event to Ophan if a Paypal payment has been made" in {

      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.pure[Future, PaypalApiError, Payment](mockPaypalPayment))
      }

      whenReady(executePayment(fixture.controller)(fakeRequest)) { _ =>
        fixture.numberOfAcquisitionEventSubmissionsShouldBe(1)
      }
    }

    "not submit an acquisition event to Ophan if a Paypal payment failed" in {

      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, PaypalApiError, Payment](Future.successful(PaypalApiError.fromString("Error"))))
      }

      whenReady(executePayment(fixture.controller)(fakeRequest)) { _ =>
        fixture.numberOfAcquisitionEventSubmissionsShouldBe(0)
      }
    }

    "submit an acquisition event to Ophan if a Paypal payment is captured" in {

      val fixture = new PaypalControllerFixture {

        Mockito.when(mockPaypalService.capturePayment(Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.pure[Future, PaypalApiError, Capture](mock[Capture]))
      }

      val result = Helpers.call(fixture.controller.capturePayment, captureRequest)

      whenReady(result) { _ =>
        fixture.numberOfAcquisitionEventSubmissionsShouldBe(1)
      }
    }

    "not submit an acquisition event to Ophan if a Paypal payment failed to be captured" in {

      val fixture = new PaypalControllerFixture {

        Mockito.when(mockPaypalService.capturePayment(Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, PaypalApiError, Capture](Future.successful(PaypalApiError.fromString("Error"))))
      }

      val result = Helpers.call(fixture.controller.capturePayment, captureRequest)

      whenReady(result) { _ =>
        fixture.numberOfAcquisitionEventSubmissionsShouldBe(0)
      }
    }
  }
}
