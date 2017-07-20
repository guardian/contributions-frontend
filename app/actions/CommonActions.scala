package actions

import abtests.Test
import abtests.Variant
import controllers.{Cached, NoCache}
import models.PaymentProvider
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

object CommonActions {

  val NoCacheAction = resultModifier(NoCache(_))

  val CachedAction = resultModifier(Cached(_))

  private def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }

  def SharedSecretAction[A](secret: String, parameterName: String = "api-key")(action: Action[A]) = new Action[A] {
    override def parser: BodyParser[A] = action.parser

    override def apply(request: Request[A]): Future[Result] = {
      if (request.getQueryString(parameterName).contains(secret)) {
        action.apply(request)
      } else {
        Future.successful(Results.Forbidden)
      }
    }
  }

  case class ABTestRequest[A](testId: Int, request: Request[A]) extends WrappedRequest(request) {
    val testAllocations = Test.allocations(testId, request)
    def isAllocated(test: Test, variantName: String) = testAllocations.exists(a => a.test == test && a.variant.name == variantName)

    def getVariant(test: Test): Option[Variant] =
      testAllocations.find(_.test == test).map(_.variant)
  }

  object ABTestAction extends ActionBuilder[ABTestRequest] {
    override def invokeBlock[A](request: Request[A], block: (ABTestRequest[A]) => Future[Result]): Future[Result] = {
      val idFromCookie: Option[Int] = request.cookies.get(Test.testIdCookieName).map(_.value.toInt)
      val testId: Int = idFromCookie.getOrElse(Random.nextInt(Test.maxTestId))

      block(ABTestRequest(testId, request)).map { result =>
        if (idFromCookie.isEmpty) result.withCookies(Test.idCookie(testId))
        else result
      }
    }
  }

  implicit class MobileSupportRequest[A](val request: Request[A]) extends AnyVal {
    def platform: String = request.getQueryString("platform").orElse(request.session.get("platform")).getOrElse("web")
    def isAndroid: Boolean = platform == "android"
    def isIos: Boolean = platform == "ios"
    def isMobile: Boolean = isAndroid || isIos
  }

  object MobileSupportAction extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      implicit val r = request
      val platform = request.getQueryString("platform") orElse request.session.get("platform")
      block(request).map { result =>
        platform match {
          case Some(value) => result.addingToSession("platform" -> value)
          case None => result
        }
      }
    }
  }

  implicit class PaymentProviderSupportRequest[A](val request: Request[A]) extends AnyVal {
    def paymentProvider: Option[PaymentProvider] =
      request.session.get(PaymentProvider.sessionKey).flatMap(PaymentProvider.withNameOption)
  }
}
