package actions

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.FakeRequest

import scala.concurrent.Future

class BasicAuthSpec extends PlaySpec with MustMatchers with Results {

  "The basic auth parser" must {
    "return None when there's no authorisation" in {
      val request = FakeRequest("", "")
      BasicAuth.parseAuthorisation(request) mustBe None
    }
    "return parsed credentials when there's an authorization header" in {
      val request = FakeRequest("", "").withHeaders("Authorization" -> "Basic dGVzdDoxMjM=")
      BasicAuth.parseAuthorisation(request) mustBe Some(BasicAuth.Credentials("test", "123"))
    }
    "return None if the header is incorrect" in {
      val request = FakeRequest("", "").withHeaders("Authorization" -> "Basic dGVzdDwq35MjM=")
      BasicAuth.parseAuthorisation(request) mustBe None
    }
  }

  private class DummyAction() extends Action[AnyContent]() {
    override def parser: BodyParser[AnyContent] = BodyParsers.parse.default
    override def apply(request: Request[AnyContent]): Future[Result] = Future.successful(Results.Ok)
  }

  "The basic auth action" must {
    "return 401 with a special header if the endpoint needs to be authenticated" in {
      val basicAuthAction = BasicAuth("test", "123")(new DummyAction)
      val result = basicAuthAction.apply(FakeRequest("", ""))
      status(result) mustBe 401
      header("WWW-Authenticate", result) mustBe Some("""Basic realm="Secured"""")
    }
    "return 200 if valid header is passed" in {
      val basicAuthAction = BasicAuth("test", "123")(new DummyAction)
      val result = basicAuthAction.apply(FakeRequest("", "").withHeaders("Authorization" -> "Basic dGVzdDoxMjM="))
      status(result) mustBe 200
    }
  }

}
