package views.support

import com.gu.i18n._
import com.gu.i18n.Currency.{all => allCurrencies}
import play.api.libs.json.{JsValue, Json}

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

  implicit class JsonCountryGroup(cg: CountryGroup) {
    val asJson: JsValue = Json.obj(
      "name" -> cg.name,
      "id" -> cg.id,
      "currency" -> cg.currency.asJson,
      "postalCode" -> cg.postalCode.name
    )
  }
}
