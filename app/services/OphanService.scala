package services

import java.net.URLEncoder

import abtests.Allocation
import enumeratum.{EnumEntry, Enum}
import models.{ContributorRow, ContributionMetaData, PaymentProvider}
import okhttp3.{HttpUrl, Request}
import play.api.{Mode, Environment}
import play.api.libs.json.{Json}
import com.gu.okhttp.RequestRunners._
import services.Ophan.{OphanError, OphanSuccess, OphanResponse}
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._

object Ophan {

  sealed trait OphanResponse

  case class OphanSuccess(response: String) extends OphanResponse

  case class OphanError(`type`: String, message: String, code: String = "", decline_code: String = "") extends Throwable with OphanResponse {
    override def getMessage: String = s"message: $message; type: ${`type`}; code: $code; decline_code: $decline_code"
  }

}


sealed trait PaymentFrequency extends EnumEntry {
  val stringValue: String
}

object PaymentFrequency extends Enum[PaymentFrequency] {

  val values = findValues

  case object OneOff extends PaymentFrequency {
    val stringValue = "ONE_OFF"
  }

  case object Monthly extends PaymentFrequency {
    val stringValue = "MONTHLY"
  }

  case object Annually extends PaymentFrequency {
    val stringValue = "ANNUALLY"
  }
}

trait Product {
  val stringValue = "CONTRIBUTION"
}

case object Contribution extends Product

case class TestData(variantName: String, complete: Boolean, campaignCodes: Option[Set[String]])

case class OphanAcquisitionEvent(
   viewId: String,
   browserId: String,
   product: Product,
   paymentFrequency: PaymentFrequency,
   currency: String,
   amount: Double,
   visitId: Option[String],
   amountInGBP: Option[Double],
   paymentProvider: Option[PaymentProvider],
   campaignCode: Option[Set[String]],
   abTests: Set[Allocation],
   countryCode: Option[String],
   referrerPageViewId: Option[String],
   referrerUrl: Option[String]
) {
  def toParams: Seq[(String, String)] = {
    Seq(
      "viewId" -> this.viewId,
      "browserId" -> this.browserId,
      "product" -> this.product.stringValue,
      "currency" -> this.currency,
      "paymentFrequency" -> this.paymentFrequency.stringValue,
      "amount" -> this.amount.toString
    ) ++
    List(
      "visitId" -> visitId.map(_.toString),
      "amountInGBP" -> amountInGBP.map(_.toString),
      "paymentProvider" -> paymentProvider.map(_.toString.toUpperCase),
      "campaignCode" -> campaignCode.map(_.mkString),
      "abTests" -> Some(OphanAcquisitionEvent.abTestToOphanJson(abTests)),
      "countryCode" -> countryCode.map(_.toString),
      "referrerPageViewId" -> referrerPageViewId.map(_.toString),
      "referrerUrl" -> referrerUrl.map(_.toString)
    ).collect{ case (k, Some(v)) => k -> v  }
  }
}



object OphanAcquisitionEvent {

  def apply(contributionMetaData: ContributionMetaData, contributorRow: ContributorRow, convertedAmount: Option[Double], paymentProvider: PaymentProvider): Option[OphanAcquisitionEvent] = {

    val campaignCodes = List(contributionMetaData.cmp, contributionMetaData.intCmp).sequence[Option, String].map(_.toSet)

    for {
      viewId <- contributionMetaData.ophanPageviewId
      browserId <- contributionMetaData.ophanBrowserId
    } yield {
      OphanAcquisitionEvent(
        viewId = viewId,
        browserId = browserId,
        product = Contribution,
        currency = contributorRow.currency,
        paymentFrequency = PaymentFrequency.OneOff,
        amount = contributorRow.amount.toDouble,
        visitId = contributionMetaData.ophanVisitId,
        amountInGBP = convertedAmount,
        paymentProvider = Some(paymentProvider),
        campaignCode = campaignCodes,
        abTests = contributionMetaData.abTests,
        countryCode = contributionMetaData.country,
        referrerPageViewId = contributionMetaData.refererPageviewId,
        referrerUrl = contributionMetaData.refererUrl
      )
    }
  }

  /**
    *
    * @param abTest: at Set of ab test Allocations
    * @return String: A JSON string in the format
    *    {"<testName>": {"variantName": "<testVariant>"}, "<testName2>": {"variantName": "<testVariant2>"}}
    */

  def abTestToOphanJson(abTest: Set[Allocation]): String = {
    val data = abTest
      .map(_.toOphanJson)
      .reduce(_ ++ _)

    Json.stringify(data)
  }
}

class OphanService (client: LoggingHttpClient[Future], environment: Environment)(implicit ec: ExecutionContext) {
  val wsUrl = "https://ophan.theguardian.com"
  val httpClient: LoggingHttpClient[Future] = client
  val endpoint = "a.gif"

  def endpointUrl(endpoint: String, params: Seq[(String, String)] = Seq.empty): HttpUrl = {
    val withSegments = endpoint.split("/").foldLeft(urlBuilder) { case (url, segment) =>
      url.addEncodedPathSegment(segment)
    }
    params.foldLeft(withSegments) { case (url, (k, v)) =>
      val encodedkey = URLEncoder.encode(k, "UTF-8")
      val encodedvalue = URLEncoder.encode(v, "UTF-8")
      url.addEncodedQueryParameter(encodedkey, encodedvalue)
    }.build()
  }

  def urlBuilder = HttpUrl.parse(wsUrl).newBuilder()

  def submitEvent(eventData: OphanAcquisitionEvent): Future[OphanResponse] = {

    val url = endpointUrl(endpoint, Seq(eventData.toParams: _*))

    def request = new Request.Builder().url(url)
      .addHeader("Cookie", s"bwid=${eventData.browserId}; vsid=${eventData.visitId};")
      .build()

    def callOphan = for (response <- httpClient(request).run(s"${request.method} $wsUrl")) yield {
      if (response.isSuccessful) {
        OphanSuccess(response.body().string())
      } else {
        OphanError(`type` = "Error", message = s"Ophan request failed ${response.body}", code = response.code().toString)
      }
    }

    if(environment.mode == Mode.Prod) {
      callOphan
    } else {
      Future.successful(OphanSuccess("Did not call Ophan, running in DEV mode"))
    }
  }
}


