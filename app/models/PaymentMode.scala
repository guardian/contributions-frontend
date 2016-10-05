package models

sealed trait PaymentMode {
  val name: String
}

object PaymentMode {

  case object Default extends PaymentMode {
    val name = "default"
  }

  case object Testing extends PaymentMode {
    val name = "testing"
  }

  val all: Set[PaymentMode] = Set(Default, Testing)
}
