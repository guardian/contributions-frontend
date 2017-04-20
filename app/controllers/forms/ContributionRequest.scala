package controllers.forms

import com.gu.i18n.Currency
import play.api.data.{Form, FormError}
import play.api.data.format.Formatter

// THIS CASE CLASS IS USED BY THE FRONTEND AS A FORM, AND BY THE MOBILE APPS AS A JSON POST
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
  refererUrl: Option[String]
)

object ContributionRequest {
  import play.api.data.Forms._
  implicit val currencyFormatter = new Formatter[Currency] {
    type Result = Either[Seq[FormError], Currency]

    override def bind(key: String, data: Map[String, String]): Result =
      data.get(key).map(_.toUpperCase).flatMap(Currency.fromString).fold[Result](Left(Seq.empty))(currency => Right(currency))

    override def unbind(key: String, value: Currency): Map[String, String] =
      Map(key -> value.identifier)
  }

  val contributionForm: Form[ContributionRequest] = Form(
    mapping(
      "name" -> text,
      "currency" -> of[Currency],
      "amount" -> bigDecimal(10, 2),
      "email" -> email,
      "token" -> nonEmptyText,
      "marketing" -> boolean,
      "postcode" -> optional(nonEmptyText),
      "ophanPageviewId" -> text,
      "ophanBrowserId" -> optional(text),
      "cmp" -> optional(text),
      "intcmp" -> optional(text),
      "refererPageviewId" -> optional(text),
      "refererUrl" -> optional(text)
    )(ContributionRequest.apply)(ContributionRequest.unapply)
  )

}


