package models

import com.gu.exacttarget.ContributionThankYouExtension
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}


case class ContributorRow(
  email: String,
  created: DateTime,
  amount: BigDecimal,
  currency: String,
  edition: String,
  name: String,
  cardCountry: Option[String] = None
)

object ContributorRow {

  def currencyToEdition(currency: String): String = currency match {
    case "GBP" => "uk"
    case "USD" => "us"
    case "AUD" => "au"
    case _ => "international"
  }

  def fromStripe(stripeHook: StripeHook): ContributorRow = ContributorRow(
    email = stripeHook.email,
    created = stripeHook.created,
    amount = stripeHook.amount,
    currency = stripeHook.currency,
    edition = currencyToEdition(stripeHook.currency),
    name = stripeHook.name,
    cardCountry = Some(stripeHook.cardCountry)
  )

  def fromPaypal(paypalHook: PaypalHook, name: String, email: String): ContributorRow = ContributorRow(
    email = email,
    created = paypalHook.created,
    amount = paypalHook.amount,
    currency = paypalHook.currency,
    edition = currencyToEdition(paypalHook.currency),
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
            "currency" -> c.currency,
            "edition" -> c.edition,
            "name" -> c.name
          )
        )
      ),
      "DataExtensionName" -> ContributionThankYouExtension.name
    )
  }
}





