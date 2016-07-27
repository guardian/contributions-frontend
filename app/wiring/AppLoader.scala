package wiring

import play.api.ApplicationLoader.Context
import play.api._


class AppLoader extends ApplicationLoader {
  def load(context: Context) = {

    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }

    (new BuiltInComponentsFromContext(context) with AppComponents).application
  }
}
