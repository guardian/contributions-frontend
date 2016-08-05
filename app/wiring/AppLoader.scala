package wiring

import com.typesafe.config.ConfigFactory
import monitoring.SentryLogging
import play.api.ApplicationLoader.Context
import play.api._


class AppLoader extends ApplicationLoader {
  def load(context: Context) = {

    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    SentryLogging.init(ConfigFactory.load())

    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}
