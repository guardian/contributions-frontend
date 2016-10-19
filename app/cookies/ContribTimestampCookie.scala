package cookies

import java.time.Instant

import com.gu.stripe.Stripe.Charge
import com.paypal.api.payments.Payment
import configuration.Config

/**
 * Should be set when a user contributes to the Guardian.
 */
trait ContribTimestampCookie extends CookieType {

  override val name: String = "gu.contributions.contrib-timestamp"

  override val maxAge: Option[Int] = Some(Cookies.MaxAge.`10years`)

  /**
   * If in production, the guardian domain is used, so that the cookie will be accessible from
   * e.g. theguardian.com, contribute.theguardian.com
   */
  override val domain: Option[String] = if (Config.stageProd) Some(Config.guardianDomain) else Config.domain
}

object ContribTimestampCookie {

  implicit object contribTimestampCookie extends ContribTimestampCookie

  implicit val paypalPaymentValue: CookieValue[Payment, ContribTimestampCookie] =
    new CookieValue[Payment, ContribTimestampCookie] {
      override def value(data: Payment): String = data.getCreateTime
    }

  implicit val stripeChargeValue: CookieValue[Charge, ContribTimestampCookie] =
    new CookieValue[Charge, ContribTimestampCookie] {
      override def value(data: Charge): String = Instant.ofEpochSecond(data.created).toString
    }
}
