package models

import com.gu.i18n.Currency

import scala.util.Try

case class ContributionAmount(
  amount: BigDecimal,
  currency: Currency
) {
  override def toString: String = f"$amount%1.2f$currency"
}

object ContributionAmount {
  def apply(amount: String): Option[ContributionAmount] = {
    val numberString = amount.dropRight(3)
    val currencyString = amount.drop(numberString.length)
    for {
      number <- Try(BigDecimal.apply(numberString)).toOption
      currency <- Currency.fromString(currencyString)
    } yield ContributionAmount(number, currency)
  }
}
