package controllers.forms

import com.gu.i18n.Currency
import models.IdentityId
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
  refererUrl: Option[String],
  idUser: Option[IdentityId],
  platform: Option[String]
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

  implicit val identityIdFormatter = new Formatter[IdentityId] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], IdentityId] =
      data.get(key).map(IdentityId.apply).toRight(Seq(FormError(key, s"Unable to fin the key $key in the form")))
    override def unbind(key: String, value: IdentityId): Map[String, String] = Map(key -> value.id)
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
      "refererUrl" -> optional(text),
      "idUser" -> optional(of[IdentityId]),
      "platform" -> optional(text)
    )(ContributionRequest.apply)(ContributionRequest.unapply)
  )

}


