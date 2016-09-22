package model.exactTarget

import cats.data.{Xor, XorT}
import com.gu.exacttarget.DataExtension
import com.paypal.api.payments.Payment
import com.paypal.base.rest.APIContext
import models.{PaypalHook, StripeHook}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}

import scala.concurrent.Future



object ContributorRow {
  def fromStripe(stripeHook: StripeHook): ContributorRow = {

    ContributorRow(
      stripeHook.email,
      stripeHook.created,
      stripeHook.amount,
      stripeHook.currency,
      stripeHook.name,
      Some(stripeHook.cardCountry)
    ))

  }
//TODO: move these calls to the paypal service
  def fromPaypal(paypalHook: PaypalHook)(implicit apiContext: APIContext): XorT[Future, Throwable, ContributorRow] = {
    for {
      payment <- Xor.catchNonFatal(Payment.get(apiContext, paypalHook.paymentId))
      created <- Xor.catchNonFatal(new DateTime(payment.getCreateTime))
      payerInfo <- Xor.catchNonFatal(payment.getPayer.getPayerInfo)
    } yield {
      Xor.right(ContributorRow(payerInfo.getEmail, created, paypalHook.amount, paypalHook.currency, Seq(payerInfo.getFirstName, payerInfo.getMiddleName, payerInfo.getLastName).mkString(" ")))
    }
  }
}

case class ContributorRow(email: String, created: DateTime, amount: BigDecimal, currency: String, name: String, cardCountry: Option[String] = None) {
  def forExtension: DataExtension = ??? //TODO: We need to find out what DataExtension we want and add it to membership-common

  implicit val contributorRowWriter = new Writes[ContributorRow] {
    def writes(c: ContributorRow): JsValue = Json.obj(
      "To" -> Json.obj(
        "Address" -> c.email,
        "SubscriberKey" -> c.email, //this looks weird, but ExactTarget performs a self join
        "ContactAttributes" -> Json.obj(
          "SubscriberAttributes" -> Json.obj(
            "created" -> created.toString, //TODO: format this date (what does extact target need)
            "amount" -> amount,
            "currency" -> currency,
            "name" -> name
          )
        ),
        "DataExtensionName" -> forExtension.name
      )
    )
  }
}





