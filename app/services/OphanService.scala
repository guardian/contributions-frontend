package services

import abtests.Allocation
import com.gu.memsub.util.WebServiceHelper
import com.gu.monitoring.ServiceMetrics
import com.gu.okhttp.RequestRunners
import enumeratum.{EnumEntry, Enum}
import models.{ContributorRow, ContributionMetaData, PaymentProvider}
import okhttp3.Request
import play.api.libs.json.{Json, JsValue}
import com.gu.okhttp.RequestRunners._
import services.Ophan.{OphanSuccess, OphanError, OphanResponse}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import cats._
import cats.data._
import cats.implicits._

object Ophan {

  sealed trait OphanResponse

  case class OphanSuccess(response: Option[String]) extends OphanResponse

  case class OphanError(`type`: String, message: String, code: String = "", decline_code: String = "") extends Throwable with OphanResponse {
    override def getMessage: String = s"message: $message; type: ${`type`}; code: $code; decline_code: $decline_code"
  }

  implicit val ophanSuccessReads = Json.reads[OphanSuccess]
  implicit val ophanErrorReads = Json.reads[OphanError]

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


case class AbTestInfo(tests: Map[String, Allocation])

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
   abTests: JsValue,
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
      this.visitId.map(visitId => "visitId" -> this.visitId.toString) ++
      this.amountInGBP.map(amountInGBP => "amountInGBP" -> amountInGBP.toString) ++
      this.paymentProvider.map(paymentProvider => "paymentProvider" -> paymentProvider.toString.toUpperCase) ++
      this.campaignCode.map(campaignCode => "campaignCode" -> campaignCode.mkString) ++
      Seq("abTests" -> Json.stringify(abTests)) ++
      this.countryCode.map(countryCode => "countryCode" -> countryCode.toString) ++
      this.referrerPageViewId.map(referrerPageViewId => "referrerPageViewId" -> referrerPageViewId.toString) ++
      this.referrerUrl.map(referrerUrl => "referrerUrl" -> referrerUrl)
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
        visitId = None,
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
}

class OphanService(client: LoggingHttpClient[Future], browserId: String, visitId: String )(implicit ec: ExecutionContext) extends WebServiceHelper[OphanSuccess, OphanError] {
  val wsUrl = "http://ophan.theguardian.com"
  val httpClient: LoggingHttpClient[Future] = client

  override def wsPreExecute(req: Request.Builder): Request.Builder =
    req.addHeader("Cookie", s"bwid=${browserId}; vsid=${visitId};")

  def submitEvent(eventData: OphanAcquisitionEvent): Future[OphanResponse] = {
    get[OphanSuccess]("a.gif", eventData.toParams:_*)
  }
}

//TODO: Ophan in dev ?
object OphanService {
  val metrics: ServiceMetrics = new ServiceMetrics("PROD", "ophan", "tracker")
  def ophanService(browserId: String, visitId: String) = new OphanService(RequestRunners.loggingRunner(metrics), browserId, visitId)
}


