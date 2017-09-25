package services

import abtests.Allocation
import actions.CommonActions.MetaDataRequest
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import cats.data.EitherT
import com.gu.acquisition.services.OphanService
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monitoring.{LoggingTags, TagAwareLogger}
import ophan.thrift.event.{AbTest, AbTestInfo, Acquisition}
import services.ContributionOphanService.{AcquisitionSubmission, AcquisitionSubmissionBuilder}
import simulacrum.typeclass
import utils.{AttemptTo, RuntimeClassUtils, TestUserService}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

trait ContributionOphanService {

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: MetaDataRequest[_]
  ): EitherT[Future, String, AcquisitionSubmission]
}

object MockOphanService extends ContributionOphanService with TagAwareLogger with RuntimeClassUtils {

  import AcquisitionSubmissionBuilder.ops._
  import cats.instances.future._

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: MetaDataRequest[_]
  ): EitherT[Future, String, AcquisitionSubmission] =
    EitherT.fromEither[Future](a.asAcquisitionSubmission).bimap(
      err => {
        error(err)
        err
      },
      submission => {
        info(s"Acquisition submission created from instance of ${runtimeClass[A]} - $submission")
        submission
      }
    )
}

class DefaultContributionOphanService(service: OphanService)
  extends ContributionOphanService with TagAwareLogger with RuntimeClassUtils {

  import cats.instances.future._
  import actions.CommonActions._
  import AcquisitionSubmissionBuilder.ops._

  private def sendSubmission(submission: AcquisitionSubmission)(implicit executionContext: ExecutionContext) = {
    import submission._
    service.submit(acquisition, ophanIds.browserId, ophanIds.viewId, ophanIds.visitId)
      .bimap(err => s"Failed to submit acquisition event to Ophan - ${err.getMessage}", _ => submission)
  }

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: MetaDataRequest[_]
  ): EitherT[Future, String, AcquisitionSubmission] =
    EitherT.fromEither[Future](a.asAcquisitionSubmission).flatMap(sendSubmission)
      .bimap(
        err => {
          error(s"$err - contributions session id ${request.sessionId}")
          err
        },
        submission => {
          info(
            s"Acquisition submission created from an instance of ${runtimeClass[A]} and " +
            s"successfully submitted to Ophan - contributions session id ${request.sessionId}"
          )
          submission
        }
      )
}

class RequestDependentOphanService(
    default: ContributionOphanService,
    testing: ContributionOphanService,
    testUserService: TestUserService
) extends ContributionOphanService {

  override def submitAcquisition[A: AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext, tags: LoggingTags, request: MetaDataRequest[_]
  ): EitherT[Future, String, AcquisitionSubmission] =
    if (testUserService.isTestUser(request)) testing.submitAcquisition(a) else default.submitAcquisition(a)
}

object ContributionOphanService extends LazyLogging {

  def apply(config: Config, testUserService: TestUserService)(
    implicit system: ActorSystem, materializer: Materializer
  ): ContributionOphanService = {

    val ophanConfig = config.getConfig("ophan")

    lazy val productionService = {
      logger.info("Initialising production Ophan service")
      new DefaultContributionOphanService(OphanService.prod)
    }

    lazy val testService: ContributionOphanService = {
      logger.info("Initializing test Ophan service...")
      val endpoint = Try(ophanConfig.getString("testEndpoint")).toOption
      if (endpoint.isEmpty) {
        logger.info("No endpoint specified for test Ophan service. Using mock Ophan service.")
        MockOphanService
      } else {
        val uri = Try(Uri.parseAbsolute(endpoint.get)).toOption
        if (uri.isEmpty) {
          logger.error("Invalid endpoint specified for test Ophan service. Using mock Ophan service.")
          MockOphanService
        } else {
          logger.info(s"Using ${uri.get} as the endpoint for the test Ophan service.")
          new DefaultContributionOphanService(new OphanService(uri.get))
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

  case class OphanIds(browserId: String, viewId: String, visitId: Option[String])

  /**
    * Encapsulates all the data required to submit an acquisition to Ophan.
    */
  case class AcquisitionSubmission(ophanIds: OphanIds, acquisition: Acquisition)

  /**
    * Type class for creating an acquisition submission from an arbitrary data type.
    */
  @typeclass trait AcquisitionSubmissionBuilder[A] {
    import cats.syntax.either._

    def buildOphanIds(a: A): Either[String, OphanIds]

    def buildAcquisition(a: A): Either[String, Acquisition]

    def asAcquisitionSubmission(a: A): Either[String, AcquisitionSubmission] =
      for {
        ophanIds <- buildOphanIds(a)
        acquisition <- buildAcquisition(a)
      } yield AcquisitionSubmission(ophanIds, acquisition)
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
      import com.gu.acquisition.syntax._
      val abTestInfo = contributionAbTests.asAbTestInfo
      referrerAbTest.map(abTest => AbTestInfo(abTestInfo.tests + abTest)).getOrElse(abTestInfo)
    }
  }
}

