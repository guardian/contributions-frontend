package controllers

import org.scalatest.{MustMatchers, WordSpec}

class CreditCardExpiryYears$Test extends WordSpec with MustMatchers {

  "CreditCardExpiryYears$Test" should {
    "display next 10 years prefix from 2016" in {
      CreditCardExpiryYears(2016, 10) mustEqual (16 to 25).toList
    }
    "display next 10 years prefix from 2017" in {
      CreditCardExpiryYears(2017, 10) mustEqual (17 to 26).toList
    }
    "display next 9 years prefix from 2017" in {
      CreditCardExpiryYears(2017, 9) mustEqual (17 to 25).toList
    }
  }
}
