import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.FakeApplicationFactory
import play.api._
import play.core.DefaultWebCommands
import services.EmailService
import wiring.AppComponents

import scala.concurrent.ExecutionContext

trait TestComponents extends MockitoSugar {
  self: AppComponents =>

  override lazy val emailService: EmailService = mock[EmailService]
  override val jdbcExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
}

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

      (new BuiltInComponentsFromContext(context) with AppComponents with TestComponents).application
    }
  }

  override def fakeApplication(): Application =
    new TestApplicationBuilder().build()
}
