package utils

import com.gu.i18n.CountryGroup._
import com.gu.i18n.Currency

object MaxAmount {
  def forCurrency(currency: Currency): Int = currency match {
    case Australia.currency => 3500
    case _ => 2000
  }
}
