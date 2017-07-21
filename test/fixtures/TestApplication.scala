package fixtures

import play.api._
import org.scalatestplus.play.FakeApplicationFactory
import play.api.routing.Router
import play.core.DefaultWebCommands

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
