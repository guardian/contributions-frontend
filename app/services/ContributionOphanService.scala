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
import play.api.{Environment, Mode}
import services.ContributionOphanService.{AcquisitionSubmission, AcquisitionSubmissionBuilder}
import simulacrum.typeclass
import utils.{AttemptTo, RuntimeClassUtils}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

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

object ContributionOphanService extends AttemptTo with LazyLogging {

  /**
    * Attempts to build an Ophan service using an endpoint specified under the path ophan.endpoint .
    */
  def fromConfig(config: Config)(
    implicit system: ActorSystem,
    materializer: Materializer
  ): Either[String, ContributionOphanService] =
    for {
      ophanConfig <- attemptTo("get Ophan config")(config.getConfig("ophan"))
      endpoint <- attemptTo("get endpoint from Ophan config")(ophanConfig.getString("endpoint"))
      uri <- attemptTo("parse Ophan endpoint to a Uri")(Uri.parseAbsolute(endpoint))
      service = new OphanService(uri)
    } yield new DefaultContributionOphanService(service)

  /**
    * In production returns an Ophan service which sends events to the production instance of Ophan.
    *
    * Otherwise, attempts to return an Ophan service which sends events to a test endpoint -
    * specified in the config under the path - ophan.endpoint .
    * If this path is empty, then a mock Ophan service is used instead -
    * acquisition events are still built, but not sent anywhere.
    *
    * Practically ophan.endpoint should be specified if you want to run the contributions website locally against a
    * locally running instance of Ophan.
    */
  def apply(config: Config, environment: Environment)(
    implicit system: ActorSystem,
    materializer: Materializer
  ): ContributionOphanService =
    if (environment.mode == Mode.Prod) {
      new DefaultContributionOphanService(OphanService.prod)
    } else {
      fromConfig(config).valueOr { message =>
        logger.info(s"Ophan endpoint not loaded from config ($message), defaulting to mock Ophan service")
        MockOphanService
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

    protected def abTestInfo(native: Set[Allocation], nonNative: Option[AbTest]): AbTestInfo = {
      import com.gu.acquisition.syntax._
      val abTestInfo = native.asAbTestInfo
      nonNative.map(abTest => AbTestInfo(abTestInfo.tests + abTest)).getOrElse(abTestInfo)
    }
  }
}

