package utils

import com.gu.i18n.{CountryGroup, Currency}
import play.api.data.FormError
import play.api.data.format.Formatter

object Formatters {
  implicit val currencyFormatter = new Formatter[Currency] {
    type Result = Either[Seq[FormError], Currency]
    override def bind(key: String, data: Map[String, String]): Result =
      data.get(key).map(_.toUpperCase).flatMap(Currency.fromString).fold[Result](Left(Seq.empty))(currency => Right(currency))
    override def unbind(key: String, value: Currency): Map[String, String] =
      Map(key -> value.identifier)
  }

  implicit val countryGroupFormatter = new Formatter[CountryGroup] {
    type Result = Either[Seq[FormError], CountryGroup]

    override def bind(key: String, data: Map[String, String]): Result = {
      data.get(key).flatMap(CountryGroup.byId(_)).fold[Result](Left(Seq.empty))(countryGroup => Right(countryGroup))
    }
    override def unbind(key: String, value: CountryGroup): Map[String, String] = Map(key -> value.id)
  }
}
