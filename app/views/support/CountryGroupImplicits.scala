package views.support

import com.gu.i18n._
import com.gu.i18n.Currency.{all => allCurrencies}
import play.api.libs.json.{JsValue, Json, Writes}

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

  implicit val countryGroupFormat: Writes[CountryGroup] = new Writes[CountryGroup] {
    override def writes(cg: CountryGroup): JsValue = Json.obj(
      "name" -> cg.name,
      "id" -> cg.id,
      "currency" -> cg.currency.asJson,
      "postalCode" -> cg.postalCode.name
    )
  }
}
