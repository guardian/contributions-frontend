package controllers

import abtests.Allocation
import actions.CommonActions.MetaDataRequest
import akka.stream.Materializer
import cats.data.EitherT
import com.gu.i18n.{CountryGroup, GBP}
import com.paypal.api.payments.{Capture, Payment}
import configuration.CorsConfig
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
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import org.mockito.internal.verification.VerificationModeFactory
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.{JsNull, JsSuccess, JsUndefined, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class PaypalControllerFixture(implicit ec: ExecutionContext) extends MockitoSugar with Eventually {

  val mockPaymentServices: PaymentServices = mock[PaymentServices]

  val mockPaypalService: PaypalService = mock[PaypalService]
  val mockPaypalPayment: Payment = Mockito.mock[Payment](classOf[Payment], Mockito.RETURNS_DEEP_STUBS)
  val mockContributionAmount: ContributionAmount = ContributionAmount(10, GBP)

  val mockCsrfCheck: CSRFCheck = mock[CSRFCheck]

  val mockCloudwatchMetrics: CloudWatchMetrics = mock[CloudWatchMetrics]

  val mockCorsConfig = mock[CorsConfig]

  val mockOphanService = mock[ContributionOphanService]

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

  val controller: PaypalController = new PaypalController(mockPaymentServices, mockCorsConfig, mockCsrfCheck, mockCloudwatchMetrics, mockOphanService)

  def captor[A <: AnyRef](implicit classTag: ClassTag[A]): A =
    ArgumentCaptor.forClass[A](classTag.runtimeClass.asInstanceOf[Class[A]]).capture()

  def numberOfCallsToStoreMetaDataMustBe(times: Int): Unit = {
    eventually(timeout(Span(60, Seconds)), interval(Span(1, Seconds))) {
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
  }

  def numberOfAcquisitionEventSubmissionsShouldBe(times: Int): Unit =
    Mockito.verify(mockOphanService, VerificationModeFactory.times(times))
      .submitAcquisition[Any](Matchers.any[Any])(
        Matchers.any[AcquisitionSubmissionBuilder[Any]],
        Matchers.any[ClassTag[Any]],
        Matchers.any[ExecutionContext],
        Matchers.any[LoggingTags],
        Matchers.any[MetaDataRequest[_]]
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

  val fakeRequestHtml: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/").withHeaders(("Accept", "text/html"))

  val fakeRequestJson: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/").withHeaders(("Accept", "application/json"))

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
    refererAbTest = None,
    nativeAbTests = None,
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
    refererAbTest = None,
    nativeAbTests = None,
    supportRedirect = Some(true)
  )

  "Paypal Controller" should {

    "return the email address when called from support (and accept type is application/json)" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.right[Future, PaypalApiError, Payment](Future.successful(mockPaypalPayment)))
      }

      val result: Future[Result] = executeSupportPayment(fixture.controller)(fakeRequestJson)

      status(result).mustBe(200)

      (contentAsJson(result) \ "email").validate[String].mustEqual(JsSuccess("a@b.com"))
    }

    "Return an empty 200 when execute is called, accept type is application/json and supporter redirect is false" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.right[Future, PaypalApiError, Payment](Future.successful(mockPaypalPayment)))
      }

      val result: Future[Result] = executePayment(fixture.controller)(fakeRequestJson)

      val a = contentAsJson(result)
      contentAsJson(result).mustEqual(JsNull)
      status(result).mustBe(200)

    }

    "generate correct redirect URL for successful PayPal payments when accept type is text/html" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.pure[Future, PaypalApiError, Payment](mockPaypalPayment))
      }

      val result: Future[Result] = executePayment(fixture.controller)(fakeRequestHtml)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk/post-payment"))

      fixture.numberOfCallsToStoreMetaDataMustBe(1)
    }

    "generate correct redirect URL for unsuccessful PayPal payments" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, PaypalApiError, Payment](Future.successful(PaypalApiError.fromString("Error"))))
      }

      val result: Future[Result] = executePayment(fixture.controller)(fakeRequestHtml)

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

      whenReady(executePayment(fixture.controller)(fakeRequestHtml)) { _ =>
        fixture.numberOfAcquisitionEventSubmissionsShouldBe(1)
      }
    }

    "not submit an acquisition event to Ophan if a Paypal payment failed" in {

      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, PaypalApiError, Payment](Future.successful(PaypalApiError.fromString("Error"))))
      }

      whenReady(executePayment(fixture.controller)(fakeRequestHtml)) { _ =>
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
