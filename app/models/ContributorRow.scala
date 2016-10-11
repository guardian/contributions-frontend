package models

import com.gu.exacttarget.ContributionThankYouExtension
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}


case class ContributorRow(
  email: String,
  created: DateTime,
  amount: BigDecimal,
  country: String,
  name: String,
  cardCountry: Option[String] = None
)

object ContributorRow {

  def currencyToCountry(currency: String): String = currency match {
    case "GBP" => "UK"
    case "USD" => "US"
    case "AUD" => "AUS"
    case _ => "ROW"
  }

  def fromStripe(stripeHook: StripeHook): ContributorRow = ContributorRow(
    email = stripeHook.email,
    created = stripeHook.created,
    amount = stripeHook.amount,
    country = currencyToCountry(stripeHook.currency),
    name = stripeHook.name,
    cardCountry = Some(stripeHook.cardCountry)
  )

  def fromPaypal(paypalHook: PaypalHook, name: String, email: String): ContributorRow = ContributorRow(
    email = email,
    created = paypalHook.created,
    amount = paypalHook.amount,
    country = currencyToCountry(paypalHook.currency),
    name = name
  )

  implicit val contributorRowWriter = new Writes[ContributorRow] {
    def writes(c: ContributorRow): JsValue = Json.obj(
      "To" -> Json.obj(
        "Address" -> c.email,
        "SubscriberKey" -> c.email,
        "ContactAttributes" -> Json.obj(
          "SubscriberAttributes" -> Json.obj(
            "EmailAddress" -> c.email,
            "created" -> c.created.toString(),
            "amount" -> c.amount,
            "country" -> c.country,
            "name" -> c.name
          )
        )
      ),
      "DataExtensionName" -> ContributionThankYouExtension.name
    )
  }
}





