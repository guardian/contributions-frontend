package controllers

import abtests.Allocation
import cats.data.EitherT
import com.gu.i18n.{CountryGroup, GBP}
import com.paypal.api.payments.Payment
import configuration.{CorsConfig, SupportConfig}
import fixtures.TestApplicationFactory
import models.{ContributionAmount, IdentityId, SavedContributionData}
import monitoring.{CloudWatchMetrics, LoggingTags}
import org.mockito.{ArgumentCaptor, Matchers, Mockito}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.{HeaderNames, Status}
import play.api.mvc._
import play.api.test._
import play.filters.csrf.CSRFCheck
import services.{PaymentServices, PaypalService}
import cats.instances.future._
import org.mockito.internal.verification.VerificationModeFactory

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

  val supportConfig = SupportConfig("https://support.thegulocal.com/thankyou")

  Mockito.when(mockPaymentServices.paypalServiceFor(Matchers.any[Request[_]]))
    .thenReturn(mockPaypalService)

  Mockito.when(mockPaypalService.paymentAmount(mockPaypalPayment))
    .thenReturn(Some(mockContributionAmount))

  Mockito.when(mockPaypalPayment.getCreateTime)
    .thenReturn("")

  Mockito.when(mockPaypalPayment.getPayer.getPayerInfo.getEmail)
    .thenReturn("a@b.com")

  Mockito.when(mockPaypalService.storeMetaData(
    paymentId = Matchers.any[String],
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

  val controller: PaypalController = new PaypalController(mockPaymentServices, mockCorsConfig, supportConfig, mockCsrfCheck, mockCloudwatchMetrics)

  def numberOfCallsToStoreMetaDataMustBe(times: Int): Unit = {
    def captor[A <: AnyRef](implicit classTag: ClassTag[A]): A =
      ArgumentCaptor.forClass[A](classTag.runtimeClass.asInstanceOf[Class[A]]).capture()

    Mockito.verify(mockPaypalService, VerificationModeFactory.times(times)).storeMetaData(
      captor[String],
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

class PaypalControllerSpec extends PlaySpec
  with HeaderNames
  with Status
  with ResultExtractors
  with DefaultAwaitTimeout
  with TestApplicationFactory
  with BaseOneAppPerSuite {

  implicit val executionContext: ExecutionContext = app.actorSystem.dispatcher

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
    supportRedirect = Some(true)
  )

  "Paypal Controller" should {

    "generate correct redirect URL for support's successful PayPal payments" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.right[Future, String, Payment](Future.successful(mockPaypalPayment)))
      }

      val result: Future[Result] = executeSupportPayment(fixture.controller)(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("https://support.thegulocal.com/thankyou"))
    }

    "generate correct redirect URL for successful PayPal payments" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.pure[Future, String, Payment](mockPaypalPayment))
      }

      val result: Future[Result] = executePayment(fixture.controller)(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk/post-payment"))

      fixture.numberOfCallsToStoreMetaDataMustBe(1)
    }

    "generate correct redirect URL for unsuccessful PayPal payments" in {
      val fixture = new PaypalControllerFixture {
        Mockito.when(mockPaypalService.executePayment(Matchers.anyString, Matchers.anyString)(Matchers.any[LoggingTags]))
          .thenReturn(EitherT.left[Future, String, Payment](Future.successful("Error")))
      }

      val result: Future[Result] = executePayment(fixture.controller)(fakeRequest)

      status(result).mustBe(303)
      redirectLocation(result).mustBe(Some("/uk?error_code=PaypalError"))

      fixture.numberOfCallsToStoreMetaDataMustBe(0)
    }
  }
}
