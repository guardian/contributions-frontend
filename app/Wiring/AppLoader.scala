package wiring

import play.api.mvc.Cookies
import play.api.routing.Router
import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Logger}
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.libs.ws.ning.NingWSComponents
import views.support.Test


class AppLoader extends ApplicationLoader {

  def load(context: Context) = {

    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }

    new BuiltInComponentsFromContext(context) with AhcWSComponents {
      lazy val map = Map[String, Router](
        "UAT" ->  new AppComponents(AppComponents.UAT, this).router,
        "DEV" -> new AppComponents(AppComponents.DEV, this).router
      )

      lazy val router: Router = new MultiRouter((c: Cookies) =>
        c.find(_.name == "stage").map(_.value.toUpperCase).flatMap(map.get).getOrElse(map("DEV")), map("DEV"))
    }.application
  }
}
