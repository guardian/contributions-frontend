package controllers

import akka.stream.Materializer
import com.gu.i18n.Currency
import com.gu.stripe.Stripe.Charge
import com.typesafe.config.Config
import configuration.CorsConfig
import fixtures.TestApplicationFactory
import monitoring.CloudWatchMetrics
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{BaseOneAppPerSuite, PlaySpec}
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers, ResultExtractors}
import services.{ContributionOphanService, PaymentServices, StripeService}

import scala.concurrent.{ExecutionContext, Future}

class StripeControllerFixture(implicit ec: ExecutionContext) extends MockitoSugar {
  import org.mockito.Matchers._
  import org.mockito.Mockito._

  private val mockPaymentServices = mock[PaymentServices]

  val mockStripeService: StripeService = mock[StripeService]

  when(mockPaymentServices.stripeServiceFor(anyString)).thenReturn(mockStripeService)

  val mockOphanService: ContributionOphanService = mock[ContributionOphanService]

  val controller = new StripeController(
    mockPaymentServices,
    mock[Config],
    mock[CorsConfig],
    mockOphanService,
    mock[CloudWatchMetrics]
  )
}


class StripeControllerSpec extends PlaySpec
  with HeaderNames
  with Status
  with ResultExtractors
  with DefaultAwaitTimeout
  with TestApplicationFactory
  with BaseOneAppPerSuite
  with ScalaFutures {

  import Helpers._
  import org.mockito.Matchers._
  import org.mockito.Mockito._

  implicit val executionContext: ExecutionContext = app.actorSystem.dispatcher
  implicit val mat: Materializer = app.materializer

  private val payRequest = FakeRequest("POST", "/stripe/pay")
    .withJsonBody(
      Json.obj(
        "name" -> "name",
        "currency" -> "GBP",
        "amount" -> 10,
        "email" -> "test@gmail.com",
        "token" -> "token",
        "marketing" -> true,
        "ophanPageviewId" -> "ophanPageviewId"
      )
    )

  "The Stripe controller" when {

    "a payment is successfully made" should {

      "send an acquisition event to Ophan" in {

        val fixture = new StripeControllerFixture {

          when(mockStripeService.Charge.create(anyInt, any[Currency], anyString, anyString, anyString, any[Map[String, String]]))
            .thenReturn(Future.successful(mock[Charge]))
        }

//        val result = call(fixture.controller.pay, payRequest)
//
//        whenReady(result) { _ =>
//
//        }

      }
    }

    "a payment fails" should {

      "not send an acquisition event to Ophan" in {

        val fixture = new StripeControllerFixture {

          when(mockStripeService.Charge.create(anyInt, any[Currency], anyString, anyString, anyString, any[Map[String, String]]))
            .thenReturn(Future.failed(new RuntimeException("charge failed")))
        }

        val result = call(fixture.controller.pay, payRequest)


        whenReady(result) { _ =>

        }

      }
    }
  }

}
