package actions

import java.util.UUID

import abtests.Test
import abtests.Variant
import controllers.forms.ContributionRequest
import controllers.{Cached, NoCache}
import models.PaymentProvider
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
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

  case class MetaDataRequest[A](sessionId: String, testId: Int, request: Request[A]) extends WrappedRequest(request) {
    val testAllocations = Test.allocations(testId, request)
    def isAllocated(test: Test, variantName: String) = testAllocations.exists(a => a.test == test && a.variant.name == variantName)

    def getVariant(test: Test): Option[Variant] =
      testAllocations.find(_.test == test).map(_.variant)
  }

  class MetaDataAction(implicit ec: ExecutionContext) extends ActionBuilder[MetaDataRequest] {
    import MetaDataAction._

    override def invokeBlock[A](request: Request[A], block: (MetaDataRequest[A]) => Future[Result]): Future[Result] = {
      val idFromCookie: Option[Int] = request.cookies.get(Test.testIdCookieName).map(_.value.toInt)
      val testId: Int = idFromCookie.getOrElse(Random.nextInt(Test.maxTestId))

      val sessionId = request.session.get(sessionIdKey).getOrElse(UUID.randomUUID.toString)

      block(MetaDataRequest(sessionId, testId, request)).map { result =>
        val outResult = if (idFromCookie.isEmpty) result.withCookies(Test.idCookie(testId)) else result
        outResult.addingToSession(sessionIdKey -> sessionId)(request)
      }
    }
  }

  object MetaDataAction {

    val default: MetaDataAction = {
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      new MetaDataAction
    }

    val sessionIdKey = "contributions_session"
  }

  implicit class MobileSupportRequest[A](val request: Request[A]) extends AnyVal {

    private def _platform = request.getQueryString("platform").orElse(request.session.get("platform")).getOrElse("web")

    def platform: String = request.body match {
      case body: ContributionRequest => body.platform.getOrElse(_platform)
      case _ => _platform
    }

    def isAndroid: Boolean = platform == "android"
    def isIos: Boolean = platform == "ios"
    def isMobile: Boolean = isAndroid || isIos
  }

  object MobileSupportAction extends ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      block(request).map(_.addingToSession("platform" -> request.platform)(request))
    }
  }

  implicit class PaymentProviderSupportRequest[A](val request: Request[A]) extends AnyVal {
    def paymentProvider: Option[PaymentProvider] =
      request.session.get(PaymentProvider.sessionKey).flatMap(PaymentProvider.withNameOption)
  }
}
