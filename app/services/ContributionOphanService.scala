package services

import abtests.Allocation
import actions.CommonActions.MetaDataRequest
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.{ActorMaterializer, Materializer}
import cats.data.EitherT
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.OphanServiceError
import com.gu.acquisition.services.{MockOphanService, OphanService}
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monitoring.{LoggingTags, TagAwareLogger}
import ophan.thrift.event.{AbTest, AbTestInfo}
import services.OphanServiceWithLogging.LoggingContext
import utils.{AttemptTo, RuntimeClassUtils, TestUserService}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

trait ContributionOphanService {

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: MetaDataRequest[_]
  ): EitherT[Future, OphanServiceError, AcquisitionSubmission]
}

/**
  * Wraps an Ophan service.
  * Useful if you want to provide different logging dependent on whether the underlying service
  * e.g. whether its attempting to send acquisition events to an Ophan endpoint, in addition to building them
  */
class OphanServiceWithLogging(service: OphanService, infoMessage: LoggingContext => String)
  extends ContributionOphanService with TagAwareLogger with RuntimeClassUtils {

  import cats.instances.future._

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: MetaDataRequest[_]
  ): EitherT[Future, OphanServiceError, AcquisitionSubmission] =
    service.submit(a).bimap(
      err => {
        error(s"${err.getMessage} - contributions session id ${request.sessionId}")
        err
      },
      submission => {
        val context = LoggingContext(runtimeClass[A], request, submission)
        info(infoMessage(context))
        submission
      }
    )
}

object OphanServiceWithLogging {

  /**
    * Context required to for useful logging messages on a successful acquisition submission.
    */
  private[services] case class LoggingContext(
    runtimeClass: Class[_],
    request: MetaDataRequest[_],
    submission: AcquisitionSubmission
  )

  def io(uri: Uri)(implicit system: ActorSystem, mat: Materializer): OphanServiceWithLogging =
    new OphanServiceWithLogging(OphanService(uri), { ctx =>
      s"Acquisition submission created from an instance of ${ctx.runtimeClass} and " +
      s"successfully submitted to Ophan - contributions session id ${ctx.request.sessionId}"
    })

  def mock: OphanServiceWithLogging = new OphanServiceWithLogging(MockOphanService, { ctx =>
    s"Acquisition submission created from an instance of ${ctx.runtimeClass} - submission: ${ctx.submission}"
  })
}

class RequestDependentOphanService(
  default: OphanServiceWithLogging,
  testing: OphanServiceWithLogging,
  testUserService: TestUserService
) extends ContributionOphanService with TagAwareLogger with RuntimeClassUtils {

  override def submitAcquisition[A: AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext, tags: LoggingTags, request: MetaDataRequest[_]
  ): EitherT[Future, OphanServiceError, AcquisitionSubmission] =
    if (testUserService.isTestUser(request)) testing.submitAcquisition(a) else default.submitAcquisition(a)
}

object ContributionOphanService extends LazyLogging {

  def apply(config: Config, testUserService: TestUserService)(
    implicit system: ActorSystem, materializer: Materializer
  ): ContributionOphanService = {

    val ophanConfig = config.getConfig("ophan")

    lazy val productionService = {
      logger.info("Initialising production Ophan service")
      OphanServiceWithLogging.io(OphanService.prodEndpoint)
    }

    lazy val testService = {
      logger.info("Initializing test Ophan service...")
      val endpoint = Try(ophanConfig.getString("testEndpoint")).toOption
      if (endpoint.isEmpty) {
        logger.info("No endpoint specified for test Ophan service. Using mock Ophan service.")
        OphanServiceWithLogging.mock
      } else {
        val uri = Try(Uri.parseAbsolute(endpoint.get)).toOption
        if (uri.isEmpty) {
          logger.error("Invalid endpoint specified for test Ophan service. Using mock Ophan service.")
          OphanServiceWithLogging.mock
        } else {
          logger.info(s"Using ${uri.get} as the endpoint for the test Ophan service.")
          OphanServiceWithLogging.io(uri.get)
        }
      }
    }

    val testing = ophanConfig.getString("testing") match {
      case "LIVE" =>
        logger.warn("Using production Ophan service for test requests")
        productionService
      case "TEST" =>
        logger.info("Using test Ophan service for test requests")
        testService
    }

    val default = ophanConfig.getString("default") match {
      case "LIVE" =>
        logger.info("Using production Ophan service for default requests")
        productionService
      case "TEST" =>
        logger.info("Using test Ophan service for default requests")
        testService
      }

    new RequestDependentOphanService(default, testing, testUserService)
  }

  trait ContributionAcquisitionSubmissionBuilder[A] extends AcquisitionSubmissionBuilder[A]
    with AttemptTo with RuntimeClassUtils {

    protected def attemptToGet[B](field: String)(a: => B)(implicit classTag: ClassTag[A]): Either[String, B] =
      attemptTo[B](s"get $field")(a).leftMap { err =>
        s"Failed to build an acquisition submission from an instance of ${runtimeClass[A]} - cause: $err"
      }

    /**
      * Combine the tests the user is in on the contributions website and the referring page.
      */
    protected def abTestInfo(contributionAbTests: Set[Allocation], referrerAbTest: Option[AbTest]): AbTestInfo = {
      import com.gu.acquisition.syntax.iterable._
      val abTestInfo = contributionAbTests.asAbTestInfo
      referrerAbTest.map(abTest => AbTestInfo(abTestInfo.tests + abTest)).getOrElse(abTestInfo)
    }
  }
}

