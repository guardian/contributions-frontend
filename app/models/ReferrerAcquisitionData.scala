package models

import java.net.URLDecoder

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc.QueryStringBindable

/**
  * Model for acquisition data passed by the referrer.
  * This should be included in the request to the contribution website as part of the query string: acquisitionData={}
  * The value should be the data encoded using Json in the canonical way, and then percent encoded.
  */
case class ReferrerAcquisitionData(
    campaignCode: Option[String],
    referrerPageviewId: Option[String],
    referrerUrl: Option[String],
    componentId: Option[String],
    componentType: Option[ComponentType],
    source: Option[AcquisitionSource],
    // Used to store the option of the client being in a test on the referring page,
    // that resulted on them landing on the contributions page.
    // e.g. they clicked the contribute link in an Epic AB test.
    abTest: Option[AbTest]
)

object ReferrerAcquisitionData {
  import cats.syntax.either._
  import utils.QueryStringBindableUtils._
  import utils.ThriftUtils.Implicits._ // Ignore IntelliJ - this is used!

  val queryStringKey = "acquisitionData"

  implicit val acquisitionDataFormat: Format[ReferrerAcquisitionData] = Json.format[ReferrerAcquisitionData]

  private val acquisitionDataQueryStringBindable: QueryStringBindable[ReferrerAcquisitionData] =
    queryStringBindableInstanceFromFormat[ReferrerAcquisitionData]

  def fromQueryString(queryString: Map[String, Seq[String]]): Either[String, ReferrerAcquisitionData] =
    acquisitionDataQueryStringBindable.bind(queryStringKey, queryString)
      .getOrElse(Either.left(s"$queryStringKey not found in query string"))
}
