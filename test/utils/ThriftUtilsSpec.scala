package utils

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.AcquisitionSource
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, JsValue, Reads}

class ThriftUtilsSpec extends PlaySpec with EitherValues {
  import ThriftUtils.Implicits._

  def formatCheck[A](enum: String, instance: A)(implicit F: ThriftEnumFormatter[A]): Unit =
    F.decode(enum).right.value mustEqual instance

  def jsonCheck[A : Reads](enum: String, instance: A): Unit =
    (JsString(enum): JsValue).validate[A].asEither.right.value mustEqual instance

  "A component type" should {

    "be able to be decoded from a string representation" in {
      import ophan.thrift.componentEvent.ComponentType._

      // Some common component types used by acquisitions
      formatCheck[ComponentType]("ACQUISITIONS_EPIC", AcquisitionsEpic)
      formatCheck[ComponentType]("ACQUISITIONS_ENGAGEMENT_BANNER", AcquisitionsEngagementBanner)
      formatCheck[ComponentType]("ACQUISITIONS_THRASHER", AcquisitionsThrasher)
    }

    "be able to be read from JSON" in {
      import ophan.thrift.componentEvent.ComponentType._

      // Some common component types used by acquisitions
      jsonCheck[ComponentType]("ACQUISITIONS_EPIC", AcquisitionsEpic)
      jsonCheck[ComponentType]("ACQUISITIONS_ENGAGEMENT_BANNER", AcquisitionsEngagementBanner)
      jsonCheck[ComponentType]("ACQUISITIONS_THRASHER", AcquisitionsThrasher)
    }
  }

  "An acquisition source" should {

    "be able to be decoded from a string representation" in {
      import ophan.thrift.event.AcquisitionSource._

      // Some common acquisition sources
      formatCheck[AcquisitionSource]("GUARDIAN_WEB", GuardianWeb)
      formatCheck[AcquisitionSource]("GUARDIAN_APPS", GuardianApps)
    }

    "be able to be read from JSON" in {
      import ophan.thrift.event.AcquisitionSource._

      // Some common acquisition sources
      jsonCheck[AcquisitionSource]("GUARDIAN_WEB", GuardianWeb)
      jsonCheck[AcquisitionSource]("GUARDIAN_APPS", GuardianApps)
    }
  }
}
