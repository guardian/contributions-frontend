package models

import java.util.UUID

import models.PaymentProvider.Paypal
import models.PaymentStatus.Paid
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsSuccess, Json}

class PaymentHookSpec extends WordSpec with MustMatchers {

  "A Payment hook" must {
    "be able to parse paypal's json" in {

      val json = Json.parse(paypalJson)
      val jsResult = PaymentHook.paypalReader.reads(json)

      jsResult mustBe a [JsSuccess[_]]
      jsResult.get mustEqual PaymentHook(
        contributionId = UUID.fromString("2e97dfb8-8b6c-4689-87de-84d2bb8b59bf"),
        paymentId = "PAY-9D679537LH910742AK6VPPZI",
        provider = Paypal,
        created = new DateTime("2016-08-10T09:49:14Z"),
        currency = "GBP",
        cardCountry = None,
        amount = BigDecimal("22.00"),
        convertedAmount = None,
        status = Paid,
        email = None
      )
    }
  }

  lazy val paypalJson = """{
                          |    "id": "WH-4PP43238R1740622N-2MS07817V4168933B",
                          |    "event_version": "1.0",
                          |    "create_time": "2016-08-10T09:49:38Z",
                          |    "resource_type": "sale",
                          |    "event_type": "PAYMENT.SALE.COMPLETED",
                          |    "summary": "Payment completed for GBP 22.0 GBP",
                          |    "resource": {
                          |        "id": "1SN56071DG6294642",
                          |        "state": "completed",
                          |        "amount": {
                          |            "total": "22.00",
                          |            "currency": "GBP",
                          |            "details": {}
                          |        },
                          |        "payment_mode": "INSTANT_TRANSFER",
                          |        "protection_eligibility": "ELIGIBLE",
                          |        "protection_eligibility_type": "ITEM_NOT_RECEIVED_ELIGIBLE,UNAUTHORIZED_PAYMENT_ELIGIBLE",
                          |        "transaction_fee": {
                          |            "value": "1.06",
                          |            "currency": "GBP"
                          |        },
                          |        "custom": "2e97dfb8-8b6c-4689-87de-84d2bb8b59bf",
                          |        "parent_payment": "PAY-9D679537LH910742AK6VPPZI",
                          |        "create_time": "2016-08-10T09:49:14Z",
                          |        "update_time": "2016-08-10T09:49:14Z",
                          |        "links": [{
                          |            "href": "https://api.sandbox.paypal.com/v1/payments/sale/1SN56071DG6294642",
                          |            "rel": "self",
                          |            "method": "GET"
                          |        }, {
                          |            "href": "https://api.sandbox.paypal.com/v1/payments/sale/1SN56071DG6294642/refund",
                          |            "rel": "refund",
                          |            "method": "POST"
                          |        }, {
                          |            "href": "https://api.sandbox.paypal.com/v1/payments/payment/PAY-9D679537LH910742AK6VPPZI",
                          |            "rel": "parent_payment",
                          |            "method": "GET"
                          |        }]
                          |    },
                          |    "links": [{
                          |        "href": "https://api.sandbox.paypal.com/v1/notifications/webhooks-events/WH-4PP43238R1740622N-2MS07817V4168933B",
                          |        "rel": "self",
                          |        "method": "GET"
                          |    }, {
                          |        "href": "https://api.sandbox.paypal.com/v1/notifications/webhooks-events/WH-4PP43238R1740622N-2MS07817V4168933B/resend",
                          |        "rel": "resend",
                          |        "method": "POST"
                          |    }]
                          |}""".stripMargin

}
