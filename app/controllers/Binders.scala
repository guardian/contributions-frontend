package controllers


import com.gu.i18n.{Country, CountryGroup}
import play.api.mvc.PathBindable.{Parsing => PathParsing}
import play.api.mvc.QueryStringBindable.{Parsing => QueryParsing}
import scala.reflect.runtime.universe._

object Binders {
  def applyNonEmpty[A: TypeTag](f: String => A)(s: String): A =
    if (s.isEmpty) {
      val msg = s"Cannot build a ${implicitly[TypeTag[A]].tpe} from an empty string"
      throw new IllegalArgumentException(msg)
    } else f(s)

  implicit object bindableCountryGroupPathParser extends PathParsing[CountryGroup](
    id => CountryGroup.byId(id).get, _.id, (key: String, _: Exception) => s"Cannot parse path parameter $key as a CountryGroup"
  )

  implicit object bindableCountryGroupQueryParser extends QueryParsing[CountryGroup](
    id => CountryGroup.byId(id).get, _.id, (key: String, _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
  )

  implicit object bindableCountry extends QueryParsing[Country](
    alpha2 => CountryGroup.countryByCode(alpha2).get, _.alpha2, (key: String, _: Exception) => s"Cannot parse parameter $key as a Country"
  )

  implicit object bindablePaymentError extends QueryParsing[PaymentError](
    errorCode => PaymentError.fromString(errorCode).get, _.toString, (key: String, _: Exception) => s"Cannot parse parameter $key as an ErrorCode"
  )
}
