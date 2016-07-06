package wiring
import play.api.mvc.{Cookies, Handler, RequestHeader}
import play.api.routing.Router.Routes
import play.api.routing.Router

class MultiRouter(router: Cookies => Router, default: Router) extends Router {

  // I dunno what these methods are for so we just use a default router
  override def documentation: Seq[(String, String, String)] = default.documentation
  override def withPrefix(prefix: String): Router = default.withPrefix(prefix)
  override def routes: Routes = default.routes

  override def handlerFor(request: RequestHeader): Option[Handler] =
    router(request.cookies).routes.lift(request)
}
