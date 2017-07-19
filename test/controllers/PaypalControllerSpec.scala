import controllers.PaypalController
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test.Helpers.{redirectLocation, _}
import play.api.test._

class PaypalControllerSpec extends PlaySpec
  with Results
  with MockitoSugar
  with BaseOneAppPerTest
  with TestApplicationFactory {

  def fakeGET(path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, path).withHeaders(("Accept", "text/html"))

  "Paypal Controller" should {
    "generate correct redirect URL for successful PayPal payments" in {
      route(app, fakeGET("/paypal/uk/execute?paymentId=PASS&PayerID=PASS&token=123")).foreach { result =>
        status(result).mustBe(303)
        redirectLocation(result).mustBe(Some("/uk/post-payment"))
      }
    }

    "generate correct redirect URL for unsuccessful PayPal payments" in {
      route(app, fakeGET("/paypal/uk/execute?paymentId=FAIL&PayerID=FAIL&token=123")).foreach { result =>
        status(result).mustBe(303)
        redirectLocation(result).mustBe(Some("/uk?error_code=PaypalError"))
      }
    }
  }
}
