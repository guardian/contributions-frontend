package services

import abtests.Allocation
import com.gu.memsub.util.WebServiceHelper
import models.{ContributorRow, ContributionMetaData, PaymentProvider}
import okhttp3.Request
import play.api.libs.json.{Json, JsValue}
import com.gu.okhttp.RequestRunners._
import services.Ophan.{OphanError, OphanObject, OphanResponse}

import scala.concurrent.{ExecutionContext, Future}

object Ophan {

  sealed trait OphanObject

  case class OphanResponse(response: Option[String]) extends OphanObject

  case class OphanError(`type`: String, message: String, code: String = "", decline_code: String = "") extends Throwable with OphanObject{
    override def getMessage: String = s"message: $message; type: ${`type`}; code: $code; decline_code: $decline_code"
  }

  implicit val ophanResponseReads = Json.reads[OphanResponse]
  implicit val ophanError = Json.reads[OphanError]
}



trait PaymentFrequency {
  val stringValue: String
}

case object OneOff extends PaymentFrequency {
  val stringValue = "ONE_OFF"
}

case object Monthly extends PaymentFrequency {
  val stringValue = "MONTHLY"
}

case object Annually extends PaymentFrequency {
  val stringValue = "ANNUALLY"
}

trait Product {
  val stringValue = "CONTRIBUTION"
}

case object Contribution extends Product


case class AbTestInfo(tests: Map[String, Allocation])

case class TestData(variantName: String, complete: Boolean, campaignCodes: Option[Set[String]])

case class OphanAcquisitionEvent(
                                 viewId: String,
                                 visitId: String,
                                 browserId: String,
                                 product: Product,
                                 paymentFrequency: PaymentFrequency,
                                 currency: String,
                                 amount: Double,
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
      "visitId" -> this.visitId,
      "browserId" -> this.browserId,
      "product" -> this.product.stringValue,
      "currency" -> this.currency,
      "paymentFrequency" -> this.paymentFrequency.stringValue,
      "amount" -> this.amount.toString
    ) ++
      this.amountInGBP.map(amountInGBP => "amountInGBP" -> amountInGBP.toString) ++
      this.paymentProvider.map(paymentProvider => "paymentProvider" -> paymentProvider.toString) ++
      this.campaignCode.map(campaignCode => "campaignCode" -> campaignCode.toString) ++
      Seq("abTests" -> Json.stringify(abTests)) ++
      this.countryCode.map(countryCode => "countryCode" -> countryCode.toString) ++
      this.referrerPageViewId.map(referrerPageViewId => "referrerPageViewId" -> referrerPageViewId.toString) ++
      this.referrerUrl.map(referrerUrl => "referrerUrl" -> referrerUrl)
  }
}



object OphanAcquisitionEvent extends OphanObject {

  def apply(contributionMetaData: ContributionMetaData, contributorRow: ContributorRow, convertedAmount: Option[Double], paymentProvider: PaymentProvider): Option[OphanAcquisitionEvent] = {
    for {
      viewId <- contributionMetaData.ophanPageviewId
      browserId <- contributionMetaData.ophanBrowserId
      visitId <- contributionMetaData.ophanVisitId
    } yield {
      OphanAcquisitionEvent(
        viewId = viewId,
        visitId = visitId,
        browserId = browserId,
        product = Contribution,
        currency = contributorRow.currency,
        paymentFrequency = OneOff,
        amount = contributorRow.amount.toDouble,
        amountInGBP = convertedAmount,
        paymentProvider = Some(paymentProvider),
        campaignCode = Some(Set(contributionMetaData.cmp, contributionMetaData.intCmp).flatten),
        abTests = contributionMetaData.abTests,
        countryCode = contributionMetaData.country,
        referrerPageViewId = contributionMetaData.refererPageviewId,
        referrerUrl = contributionMetaData.refererUrl
      )
    }
  }
}

class OphanService(client: LoggingHttpClient[Future], eventData: OphanAcquisitionEvent)(implicit ec: ExecutionContext) extends WebServiceHelper[OphanResponse, OphanError] with OphanObject {
  val wsUrl = "https://ophan.theguardian.com"
  val httpClient: LoggingHttpClient[Future] = client

  override def wsPreExecute(req: Request.Builder): Request.Builder =
    req.addHeader("Cookie", s"bwid=${eventData.browserId}; vsid=${eventData.visitId};")

  def submitEvent = {
    get[OphanResponse]("a.gif", eventData.toParams:_*)
  }
}


