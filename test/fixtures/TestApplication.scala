package fixtures

import play.api._
import java.io.File

import org.scalatestplus.play.FakeApplicationFactory
import play.api.libs.crypto.{CSRFTokenSigner, CSRFTokenSignerProvider}
import play.api.routing.Router
import play.core.DefaultWebCommands
import play.filters.csrf.{CSRFComponents, CSRFConfig}
import wiring.AppComponents


trait TestApplicationFactory extends FakeApplicationFactory {

  class TestApplicationBuilder {

    def build(): Application = {
      val env = Environment.simple()
      val context = ApplicationLoader.Context(
        environment = env,
        sourceMapper = None,
        webCommands = new DefaultWebCommands(),
        initialConfiguration = Configuration.load(env)
      )

      new BuiltInComponentsFromContext(context) {
        override def router: Router = Router.empty
      }.application
    }
  }

  override def fakeApplication(): Application =
    new TestApplicationBuilder().build()
}
