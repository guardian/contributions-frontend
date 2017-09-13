package models

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.data.format.Formatter
import play.api.libs.json.{Format, Json}

/**
  * Model for acquisition data passed by the referrer.
  * This should be included in the request to the contribution website as part of the query string: acquisitionData={}
  * The value should be the data encoded using Json in the canonical way, and then percent encoded.
  */
case class ReferrerAcquisitionData(
    campaignCode: Option[String] = None,
    referrerPageviewId: Option[String] = None,
    componentId: Option[String] = None,
    componentType: Option[ComponentType] = None,
    source: Option[AcquisitionSource] = None,
    // Test the client was in on the referring page,
    // that resulted on them landing on the contributions page.
    // e.g. they clicked the contribute link in the Epic.
    abTest: Option[AbTest] = None
)

object ReferrerAcquisitionData {
  import utils.ThriftUtils.Implicits._

  def fromQueryString(queryString: Map[String, Seq[String]]): Either[String, ReferrerAcquisitionData] = ???

  def empty: ReferrerAcquisitionData = ReferrerAcquisitionData()

  implicit val acquisitionDataFormat: Format[ReferrerAcquisitionData] = Json.format[ReferrerAcquisitionData]

  implicit val acquisitionDataFormatter: Formatter[ReferrerAcquisitionData] = ???
}
