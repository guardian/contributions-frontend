package controllers.forms

import com.gu.i18n.Currency
import models.IdentityId
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource, Platform}
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}


// THIS CASE CLASS IS USED BY THE FRONTEND AND BY THE MOBILE APPS AS A JSON POST
// NEW FIELDS SHOULD BE OPTIONAL OR SUPPORTED BY BOTH
case class ContributionRequest(
  name: String,
  currency: Currency,
  amount: BigDecimal,
  email: String,
  token: String,
  marketing: Boolean,
  postcode: Option[String],
  ophanPageviewId: String,
  ophanBrowserId: Option[String],
  cmp: Option[String],
  intcmp: Option[String],
  refererPageviewId: Option[String],
  refererUrl: Option[String],
  idUser: Option[IdentityId],
  platform: Option[String],
  ophanVisitId: Option[String],
  componentId: Option[String],
  componentType: Option[ComponentType],
  source: Option[AcquisitionSource],
  refererAbTest: Option[AbTest],
  nativeAbTests: Option[Set[AbTest]],
  isSupport: Option[Boolean]
)

object ContributionRequest {
  import utils.ThriftUtils.Implicits._ // Ignore IntelliJ - this is used!

  private implicit val currencyReads: Reads[Currency] = Reads { json =>
    json.validate[String].flatMap { currency =>
      Currency.fromString(currency.toUpperCase)
        .map(JsSuccess(_))
        .getOrElse(JsError(s"Invalid currency: $currency"))
    }
  }

  implicit val contributionRequestReads: Reads[ContributionRequest] = Json.reads[ContributionRequest]
}

