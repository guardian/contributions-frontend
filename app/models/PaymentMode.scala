package models

import enumeratum._

sealed trait PaymentMode extends EnumEntry

object PaymentMode extends Enum[PaymentMode] {
  val values = findValues

  case object Default extends PaymentMode
  case object Testing extends PaymentMode
}
