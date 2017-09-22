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
import utils.{AttemptTo, RuntimeClassUtils}

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

object ContributionOphanService extends LazyLogging {

  /**
    * Expects the config to have the path ophan.isProd defining a boolean.
    */
  def fromConfig(config: Config)(implicit system: ActorSystem, materializer: Materializer): ContributionOphanService = {
    val ophanConfig = config.getConfig("ophan")
    if (ophanConfig.getBoolean("isProd")) {
      logger.info("Initialising production instance of Contribution Ophan service.")
      new DefaultContributionOphanService(OphanService.prod)
    } else {
      val testEndpoint = Try(ophanConfig.getString("endpoint")).toOption
      if (testEndpoint.isEmpty) {
        logger.info("No test endpoint specified. Initialising a mock Contribution Ophan service.")
        MockOphanService
      } else {
        val uri = Try(Uri.parseAbsolute(testEndpoint.get)).toOption
        if (uri.isEmpty) {
          logger.warn("Invalid test Ophan endpoint specified. Initialising a mock Contribution Ophan service.")
          MockOphanService
        } else {
          logger.info(s"Initialising test instance of Contribution Ophan service with endpoint: ${uri.get}")
          new DefaultContributionOphanService(new OphanService(uri.get))
        }
      }
    }
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

