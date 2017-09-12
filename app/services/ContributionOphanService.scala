package services

import abtests.Allocation
import actions.CommonActions.ABTestRequest
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.EitherT
import cats.syntax.EitherSyntax
import cats.syntax.either._
import com.gu.acquisition.services.{OphanService, OphanServiceError}
import com.paypal.api.payments.Payment
import controllers.httpmodels.CaptureRequest
import monitoring.{LoggingTags, TagAwareLogger}
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AbTestInfo, Acquisition, AcquisitionSource}
import play.api.{Environment, Mode}
import services.ContributionOphanService.{AcquisitionSubmissionBuilder, AcquisitionSubmissionBuilderUtils, OphanIds}
import simulacrum.typeclass

import scala.concurrent.{ExecutionContext, Future}

/**
  * Used to send contribution acquisitions to Ophan.
  */
class ContributionOphanService(env: Environment)(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends TagAwareLogger {

  /**
    * A left-valued result is returned iff the app is in production mode and the event was not successfully sent.
    */
  // TODO: should an error be returned if not in production mode?
  def submitAcquisition[A : AcquisitionSubmissionBuilder](a: A)(
    implicit ec: ExecutionContext, loggingTags: LoggingTags): EitherT[Future, OphanServiceError, Unit] = {

    import cats.instances.future._
    import ContributionOphanService._
    import AcquisitionSubmissionBuilder.ops._

    EitherT.fromEither[Future](a.asAcquisitionSubmission)
      .flatMap {
        // Ophan should filter out events coming from the Guardian IP addresses.
        // Still, explicitly check the server is running in production mode as an extra precaution,
        // before submitting the event.
        case AcquisitionSubmission(ophanIds, acquisition) if env.mode == Mode.Prod =>
          OphanService.submit(acquisition, ophanIds.browserId, ophanIds.viewId, ophanIds.visitId).map(_ => ())
        case _ =>
          EitherT.pure[Future, OphanServiceError, Unit](())
      }
      .bimap(
        err => {
          error("Failed to submit acquisition event to Ophan", err)
          err
        },
        result => {
          info("Acquisition event sent to Ophan")
          result
        }
      )
  }
}

object ContributionOphanService {

  case class OphanIds(browserId: String, viewId: String, visitId: Option[String])

  /**
    * Encapsulates all the data required to submit an acquisition to Ophan.
    */
  case class AcquisitionSubmission(ophanIds: OphanIds, acquisition: Acquisition)

  /**
    * Type class for creating an acquisition submission from an arbitrary data type.
    */
  @typeclass trait AcquisitionSubmissionBuilder[A] {

    def buildOphanIds(a: A): Either[OphanServiceError, OphanIds]

    def buildAcquisition(a: A): Either[OphanServiceError, Acquisition]

    def asAcquisitionSubmission(a: A): Either[OphanServiceError, AcquisitionSubmission] =
      for {
        ophanIds <- buildOphanIds(a)
        acquisition <- buildAcquisition(a)
      } yield AcquisitionSubmission(ophanIds, acquisition)
  }

  /**
    * Mixin to facilitate creating acquisition submission builders.
    */
  trait AcquisitionSubmissionBuilderUtils extends EitherSyntax {

    def tryField[A](name: String)(a: => A): Either[OphanServiceError, A] =
      Either.catchNonFatal(a).leftMap { _ =>
        OphanServiceError.Generic(new RuntimeException(s"unable to get value for field $name"))
      }

    def abTestInfo(native: Set[Allocation], nonNative: Option[ophan.thrift.event.AbTest]): ophan.thrift.event.AbTestInfo = {
      import com.gu.acquisition.syntax._
      val abTestInfo = native.asAbTestInfo
      nonNative.map(abTest => AbTestInfo(abTestInfo.tests + abTest)).getOrElse(abTestInfo)
    }
  }
}

