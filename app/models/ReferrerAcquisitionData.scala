package models

package models

import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.data.format.Formatter
import play.api.libs.json.{Format, Json}
import utils.QueryStringBindableUtils.QueryParamsFormat

/**
  * Model for acquisition data passed by the referrer.
  * This should be included in the request to the contribution website as part of the query string: acquisitionData={}
  * The value should be the data encoded using Json in the canonical way, and then percent encoded.
  *
  *
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

  def empty: ReferrerAcquisitionData = ReferrerAcquisitionData(
    campaignCode = None,
    referrerPageviewId = None,
    componentId = None,
    componentType = None,
    source = None,
    abTest = None
  )

  implicit val acquisitionDataFormat: Format[ReferrerAcquisitionData] = Json.format[ReferrerAcquisitionData]

  implicit val acquisitionDataFormatter: Formatter[ReferrerAcquisitionData] = ???

  implicit val acquisitionDataQueryStringFormat: QueryParamsFormat[ReferrerAcquisitionData] =
    new QueryParamsFormat[ReferrerAcquisitionData] {
      import cats.syntax.either._

      private val encoding = "utf-8"

      override val key: String = "acquisitionData"

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ReferrerAcquisitionData]] =
        params.get(key).map { fields =>
          Either.fromOption(fields.headOption, s"key $key not included in query string")
            .flatMap { json =>
              val decodedJson = java.net.URLDecoder.decode(json, encoding)
              Json.parse(decodedJson).validate[ReferrerAcquisitionData].asEither
                .leftMap(_ => s"json $decodedJson is not a valid encoding of referrer acquisition data")
            }
        }

      override def unbind(key: String, value: ReferrerAcquisitionData): String =
        key + "=" + java.net.URLEncoder.encode(Json.toJson(value).toString, encoding)
    }
}
