package models

import models.PaymentProvider.{Paypal, Stripe}
import models.PaymentStatus.Paid
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsSuccess, Json}
import models.PaymentMode.Testing

class PaymentHookSpec extends WordSpec with MustMatchers {

  "A Payment hook" must {
    "be able to parse paypal's json" in {

      val json = Json.parse(paypalJson)
      val jsResult = PaypalHook.reader.reads(json)

      jsResult mustBe a [JsSuccess[_]]
      jsResult.get mustEqual PaypalHook(
        contributionId = ContributionId("2e97dfb8-8b6c-4689-87de-84d2bb8b59bf"),
        paymentId = "PAY-9D679537LH910742AK6VPPZI",
        created = new DateTime("2016-08-10T09:49:14Z"),
        currency = "GBP",
        amount = BigDecimal("22.00"),
        status = Paid
      )
    }
    "be able to parse paypal's json and convert it to a payment hook" in {

      val json = Json.parse(paypalJson)
      val jsResult = PaypalHook.reader.reads(json).map(PaymentHook.fromPaypal)

      jsResult mustBe a [JsSuccess[_]]
      jsResult.get mustEqual PaymentHook(
        contributionId = ContributionId("2e97dfb8-8b6c-4689-87de-84d2bb8b59bf"),
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

    "be able to parse stripe's json" in {

      val json = Json.parse(stripeJson)
      val stripeHook = StripeHook.reader.reads(json)

      stripeHook mustBe a [JsSuccess[_]]
      stripeHook.get mustEqual StripeHook(
        contributionId = ContributionId("7f5256d2-8e63-4b29-8f1e-f5c4e670db22"),
        eventId = "evt_18u3jGCbpG0cQtlb9k9WRx6f",
        paymentId = "ch_18u3jGCbpG0cQtlbhYCnPcuz",
        mode = Testing,
        created = new DateTime("2016-09-15T17:32:54Z"),
        currency = "GBP",
        amount = BigDecimal("25.00"),
        cardCountry = "US",
        status = Paid,
        email = "a@a.a"
      )
    }

    "be able to parse stripe's json and convert it to a payment hook" in {

      val json = Json.parse(stripeJson)
      val stripeHook = StripeHook.reader.reads(json)

      val jsResult = stripeHook.map { stripeHook =>
        PaymentHook.fromStripe(
          stripeHook = stripeHook,
          convertedAmount = Some(BigDecimal("20.00"))
        )
      }

      jsResult mustBe a [JsSuccess[_]]
      jsResult.get mustEqual PaymentHook(
        contributionId = ContributionId("7f5256d2-8e63-4b29-8f1e-f5c4e670db22"),
        paymentId = "ch_18u3jGCbpG0cQtlbhYCnPcuz",
        provider = Stripe,
        created = new DateTime("2016-09-15T17:32:54Z"),
        currency = "GBP",
        cardCountry = Some("US"),
        amount = BigDecimal("25.00"),
        convertedAmount = Some(BigDecimal("20.00")),
        status = Paid,
        email = Some("a@a.a")
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


  lazy val stripeJson = """{
                          |  "id": "evt_18u3jGCbpG0cQtlb9k9WRx6f",
                          |  "object": "event",
                          |  "api_version": "2016-03-07",
                          |  "created": 1473960774,
                          |  "data": {
                          |    "object": {
                          |      "id": "ch_18u3jGCbpG0cQtlbhYCnPcuz",
                          |      "object": "charge",
                          |      "amount": 2500,
                          |      "amount_refunded": 0,
                          |      "application_fee": null,
                          |      "balance_transaction": "txn_18u3jGCbpG0cQtlbFz3nsClh",
                          |      "captured": true,
                          |      "created": 1473960774,
                          |      "currency": "gbp",
                          |      "customer": null,
                          |      "description": "Your contribution",
                          |      "destination": null,
                          |      "dispute": null,
                          |      "failure_code": null,
                          |      "failure_message": null,
                          |      "fraud_details": {},
                          |      "invoice": null,
                          |      "livemode": false,
                          |      "metadata": {
                          |        "marketing-opt-in": "true",
                          |        "ophanId": "it4m6aomfvm3rlv0o1ov",
                          |        "abTests": "[{\"testName\":\"AmountHighlightTest\",\"testSlug\":\"highlight\",\"variantName\":\"Amount - 50 highlight\",\"variantSlug\":\"50\"},{\"testName\":\"MessageCopyTest\",\"testSlug\":\"mcopy\",\"variantName\":\"Copy - control\",\"variantSlug\":\"control\"},{\"testName\":\"PaymentMethodTest\",\"testSlug\":\"paymentMethods\",\"variantName\":\"Paypal\",\"variantSlug\":\"paypal\"}]",
                          |        "email": "a@a.a",
                          |        "name": "a",
                          |        "idUser": "123",
                          |        "contributionId": "7f5256d2-8e63-4b29-8f1e-f5c4e670db22"
                          |      },
                          |      "order": null,
                          |      "paid": true,
                          |      "receipt_email": "a@a.a",
                          |      "receipt_number": null,
                          |      "refunded": false,
                          |      "refunds": {
                          |        "object": "list",
                          |        "data": [],
                          |        "has_more": false,
                          |        "total_count": 0,
                          |        "url": "/v1/charges/ch_18u3jGCbpG0cQtlbhYCnPcuz/refunds"
                          |      },
                          |      "shipping": null,
                          |      "source": {
                          |        "id": "card_18u3jFCbpG0cQtlbm6eZiAnX",
                          |        "object": "card",
                          |        "address_city": null,
                          |        "address_country": null,
                          |        "address_line1": null,
                          |        "address_line1_check": null,
                          |        "address_line2": null,
                          |        "address_state": null,
                          |        "address_zip": null,
                          |        "address_zip_check": null,
                          |        "brand": "Visa",
                          |        "country": "US",
                          |        "customer": null,
                          |        "cvc_check": "pass",
                          |        "dynamic_last4": null,
                          |        "exp_month": 12,
                          |        "exp_year": 2021,
                          |        "fingerprint": "6Hu7pNVx490p6mah",
                          |        "funding": "credit",
                          |        "last4": "4242",
                          |        "metadata": {},
                          |        "name": null,
                          |        "tokenization_method": null
                          |      },
                          |      "source_transfer": null,
                          |      "statement_descriptor": null,
                          |      "status": "succeeded"
                          |    }
                          |  },
                          |  "livemode": false,
                          |  "pending_webhooks": 1,
                          |  "request": "req_9CSe5vuXyXArfP",
                          |  "type": "charge.succeeded"
                          |}""".stripMargin

}
