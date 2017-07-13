package wiring

import configuration.Config
import loghandling.Logstash
import monitoring.SentryLogging
import play.api.ApplicationLoader.Context
import play.api._


class AppLoader extends ApplicationLoader {
  def load(context: Context) = {
    val config = Config
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    SentryLogging.init(config.config)
    Logstash.init(config)

    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}
