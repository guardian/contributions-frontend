package models

import java.net.URLDecoder

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.libs.json.{Json, Reads}

import scala.util.Try

/**
  * Model for acquisition data passed by the referrer.
  * This should be included in the request to the contribution website as part of the query string: acquisitionData={}
  * The value should be the data encoded using Json in the canonical way, and then percent encoded.
  */
case class ReferrerAcquisitionData(
    campaignCode: Option[String],
    referrerPageviewId: Option[String],
    componentId: Option[String],
    componentType: Option[ComponentType],
    source: Option[AcquisitionSource],
    // Test the client was in on the referring page,
    // that resulted on them landing on the contributions page.
    // e.g. they clicked the contribute link in the Epic.
    abTest: Option[AbTest]
)

object ReferrerAcquisitionData {
  import utils.ThriftUtils.Implicits._

  val queryStringKey = "acquisitionData"

  def fromQueryString(queryString: Map[String, Seq[String]]): Option[ReferrerAcquisitionData] =
    for {
      values <- queryString.get(queryStringKey)
      percentEncodedJson <- values.headOption
      json <- Try(URLDecoder.decode(percentEncodedJson, "utf-8")).toOption
      data <- Json.parse(json).validate[ReferrerAcquisitionData].asOpt
    } yield data

  implicit val acquisitionDataReads: Reads[ReferrerAcquisitionData] = Json.reads[ReferrerAcquisitionData]
}
