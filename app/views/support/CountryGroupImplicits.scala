package views.support

import com.gu.i18n._
import com.gu.i18n.Currency.{all => allCurrencies}
import play.api.libs.json._

object CountryGroupImplicits {
  implicit class JsonCurrency(c: Currency) {
    private val code: Option[String] = c match {
      case GBP => Some("gbp")
      case USD => Some("usd")
      case AUD => Some("aud")
      case EUR => Some("eur")
      case _ => None
    }

    val asJson: JsValue = Json.obj(
      "symbol" -> c.glyph,
      "prefix" -> c.prefix,
      "identifier" -> c.identifier,
      "code" -> code
    )
  }

  implicit val countryGroupReads = new Reads[CountryGroup] {
    override def reads(json: JsValue): JsResult[CountryGroup] = json match {
      case JsString(id) => CountryGroup.byId(id).map(JsSuccess(_)).getOrElse(JsError("invalid CountryGroup id"))
      case _ => JsError("invalid value for country group")
    }
  }

  implicit val countryGroupFormat: Writes[CountryGroup] = new Writes[CountryGroup] {
    override def writes(cg: CountryGroup): JsValue = Json.obj(
      "name" -> cg.name,
      "id" -> cg.id,
      "currency" -> cg.currency.asJson,
      "postalCode" -> cg.postalCode.name
    )
  }

}
