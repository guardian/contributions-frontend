package controllers.httpmodels

import com.gu.i18n.CountryGroup
import com.netaporter.uri.Uri
import com.paypal.api.payments.Payment
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.min
import play.api.libs.json._
import utils.JsonUtils._

import scala.util.Try


case class AuthRequest private (
  countryGroup: CountryGroup,
  amount: BigDecimal,
  cmp: Option[String],
  intCmp: Option[String],
  refererPageviewId: Option[String],
  refererUrl: Option[String],
  ophanPageviewId: Option[String],
  ophanBrowserId: Option[String],
  ophanVisitId: Option[String]
)

object AuthRequest {
  /**
    * We need to ensure there's no fragment in the URL here, as PayPal appends some query parameters to the end of it,
    * which will be removed by the browser (due to the URL stripping rules) in its requests.
    *
    * See: https://www.w3.org/TR/referrer-policy/#strip-url
    *
    */
  def withSafeRefererUrl(
    countryGroup: CountryGroup,
    amount: BigDecimal,
    cmp: Option[String],
    intCmp: Option[String],
    refererPageviewId: Option[String],
    refererUrl: Option[String],
    ophanPageviewId: Option[String],
    ophanBrowserId: Option[String],
    ophanVisitId: Option[String]
  ): AuthRequest = {
    val safeRefererUrl = refererUrl.flatMap(url => Try(Uri.parse(url).copy(fragment = None).toString).toOption)

    new AuthRequest(countryGroup, amount, cmp, intCmp, refererPageviewId, safeRefererUrl, ophanPageviewId, ophanBrowserId, ophanVisitId)
  }

  implicit val authRequestReads: Reads[AuthRequest] = (
    (__ \ "countryGroup").read[CountryGroup] and
      (__ \ "amount").read(min[BigDecimal](1)) and
      (__ \ "cmp").readNullable[String] and
      (__ \ "intCmp").readNullable[String] and
      (__ \ "refererPageviewId").readNullable[String] and
      (__ \ "refererUrl").readNullable[String] and
      (__ \ "ophanPageviewId").readNullable[String] and
      (__ \ "ophanBrowserId").readNullable[String] and
      (__ \ "ophanVisitId").readNullable[String]
    ) (AuthRequest.withSafeRefererUrl _)
}

case class AuthResponse(approvalUrl: Uri, paymentId: String)

object AuthResponse {
  import cats.syntax.either._

  import scala.collection.JavaConverters._

  def fromPayment(payment: Payment): Either[String, AuthResponse] = Either.fromOption(for {
    links <- Option(payment.getLinks)
    approvalLinks <- links.asScala.find(_.getRel.equalsIgnoreCase("approval_url"))
    approvalUrl <- Option(approvalLinks.getHref)
    paymentId <- Option(payment.getId)
  } yield AuthResponse(Uri.parse(approvalUrl), paymentId), "Unable to parse payment")

  implicit val uriWrites = new Writes[Uri] {
    override def writes(uri: Uri): JsValue = JsString(uri.toString)
  }

  implicit val authResponseWrites: Writes[AuthResponse] = Json.writes[AuthResponse]
}
