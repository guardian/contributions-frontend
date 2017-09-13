package models

import java.net.URLDecoder

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.QueryStringBindable

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
  import cats.syntax.either._
  import utils.QueryStringBindableUtils._
  import utils.ThriftUtils.Implicits._ // Ignore IntelliJ - this is used!

  val queryStringKey = "acquisitionData"

  implicit val acquisitionDataReads: Reads[ReferrerAcquisitionData] = Json.reads[ReferrerAcquisitionData]

  private val acquisitionDataQueryStringBindable: QueryStringBindable[ReferrerAcquisitionData] = {
    implicit val acquisitionDataWrites: Writes[ReferrerAcquisitionData] = Json.writes[ReferrerAcquisitionData]
    queryStringBindableInstanceFromFormat[ReferrerAcquisitionData]
  }

  def fromQueryString(queryString: Map[String, Seq[String]]): Either[String, ReferrerAcquisitionData] =
    acquisitionDataQueryStringBindable.bind(queryStringKey, queryString)
      .getOrElse(Either.left(s"$queryStringKey not found in query string"))
}
