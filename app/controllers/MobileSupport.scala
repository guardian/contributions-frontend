package controllers

import play.api.mvc.{ActionBuilder, Request, Result, WrappedRequest}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

case class MobileSupportRequest[A](request: Request[A], platform: Option[String]) extends WrappedRequest(request) {
  def isMobile: Boolean = platform.contains("android") || platform.contains("ios")
}

object MobileSupportAction extends ActionBuilder[MobileSupportRequest] {
  override def invokeBlock[A](request: Request[A], block: (MobileSupportRequest[A]) => Future[Result]): Future[Result] = {
    implicit val r = request
    val platform = request.getQueryString("platform") orElse request.session.get("platform")
    val wrappedRequest = MobileSupportRequest(request, platform)
    block(wrappedRequest).map { result =>
      platform match {
        case Some(value) => result.addingToSession("platform" -> value)
        case None => result
      }
    }
  }
}
