package models

import java.net.URLDecoder

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.libs.json.{Json, Reads}

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

  def fromQueryString(queryString: Map[String, Seq[String]]): Either[String, ReferrerAcquisitionData] = {
    import cats.syntax.either._

    for {

      values <- Either.fromOption(
        queryString.get(queryStringKey),
        s"query string key $queryStringKey not found"
      )

      percentEncodedJson <- Either.fromOption(
        values.headOption,
        s"query string key $queryStringKey empty"
      )

      json <- Either.catchNonFatal(URLDecoder.decode(percentEncodedJson, "utf-8"))
        .leftMap(_ => s"error decoding query string value $queryStringKey ")

      data <- Json.parse(json).validate[ReferrerAcquisitionData].asEither
        .leftMap(_ => "unable to decode acquisitions data")

    } yield data
  }

  implicit val acquisitionDataReads: Reads[ReferrerAcquisitionData] = Json.reads[ReferrerAcquisitionData]
}
