package services

import abtests.Allocation
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherT
import cats.syntax.EitherSyntax
import cats.syntax.either._
import com.gu.acquisition.services.OphanService
import monitoring.{LoggingTags, TagAwareLogger}
import ophan.thrift.event.{AbTestInfo, Acquisition}
import play.api.mvc.Request
import play.api.{Environment, Mode}
import services.ContributionOphanService.{AcquisitionSubmission, AcquisitionSubmissionBuilder}
import simulacrum.typeclass

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait ContributionOphanService {

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: Request[_]
  ): EitherT[Future, String, AcquisitionSubmission]
}

object NonProdContributionOphanService extends ContributionOphanService with TagAwareLogger {

  import AcquisitionSubmissionBuilder.ops._
  import cats.instances.future._

  private def runtimeClass[A : ClassTag] = reflect.classTag[A].runtimeClass

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: Request[_]
  ): EitherT[Future, String, AcquisitionSubmission] =
    EitherT.fromEither[Future](a.asAcquisitionSubmission).bimap(
      err => {
        error(s"Unable to build acquisition from instance of ${runtimeClass[A]} - $err")
        err
      },
      submission => {
        info(s"Acquisition submission created from instance of ${runtimeClass[A]} - $submission")
        submission
      }
    )
}

class ProdContributionOphanService(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends ContributionOphanService with TagAwareLogger {

  import cats.instances.future._
  import actions.CommonActions._
  import AcquisitionSubmissionBuilder.ops._

  private def sendSubmission(submission: AcquisitionSubmission)(implicit executionContext: ExecutionContext) = {
    import submission._
    OphanService.submit(acquisition, ophanIds.browserId, ophanIds.viewId, ophanIds.visitId)
      .bimap(err => s"Failed to submit acquisition event to Ophan - ${err.getMessage}", _ => submission)
  }

  def submitAcquisition[A : AcquisitionSubmissionBuilder : ClassTag](a: A)(
    implicit ec: ExecutionContext,
    tags: LoggingTags,
    request: Request[_]
  ): EitherT[Future, String, AcquisitionSubmission] =
    EitherT.fromEither[Future](a.asAcquisitionSubmission).flatMap(sendSubmission).leftMap { err =>
      error(s"$err - contributions session id ${request.sessionId}")
      err
    }
}

object ContributionOphanService {

  def apply(env: Environment)(implicit system: ActorSystem, mat: ActorMaterializer): ContributionOphanService =
    if (env.mode == Mode.Prod) new ProdContributionOphanService else NonProdContributionOphanService

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

  /**
    * Mixin to facilitate creating acquisition submission builders.
    */
  trait AcquisitionSubmissionBuilderUtils extends EitherSyntax {

    def tryField[A](name: String)(a: => A): Either[String, A] =
      Either.catchNonFatal(a).leftMap(err => s"unable to get value for field $name - ${err.getMessage}")

    def abTestInfo(native: Set[Allocation], nonNative: Option[ophan.thrift.event.AbTest]): ophan.thrift.event.AbTestInfo = {
      import com.gu.acquisition.syntax._
      val abTestInfo = native.asAbTestInfo
      nonNative.map(abTest => AbTestInfo(abTestInfo.tests + abTest)).getOrElse(abTestInfo)
    }
  }
}

