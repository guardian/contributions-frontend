package views.support

import com.gu.i18n.{CountryGroup, Currency}
import play.api.libs.json.{JsValue, Json}

object CountryGroupImplicits {
  implicit class JsonCurrency(c: Currency) {
    val asJson: JsValue = Json.obj(
      "symbol" -> c.glyph,
      "prefix" -> c.prefix,
      "identifier" -> c.identifier
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
