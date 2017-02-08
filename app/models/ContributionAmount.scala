package models

import com.gu.i18n.Currency

case class ContributionAmount(
  amount: BigDecimal,
  currency: Currency
) {
  override def toString: String = f"$amount%1.2f$currency"
}
