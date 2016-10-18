package utils

import java.time.Instant

import com.gu.stripe.Stripe.Charge
import com.paypal.api.payments.Payment
import play.api.Logger
import play.api.mvc.Cookie
import play.api.mvc.Result

/**
 * Type class used to infer the time a contribution was made.
 */
trait ContribEvidence[A] {

  def contribTimestamp(data: A): Instant

  /**
   * Try and get the timestamp of a contribution, reverting to [[None]] if an error was thrown.
   */
  def contribTimestampOption(data: A): Option[Instant] = {
    try {
      Option(contribTimestamp(data))
    } catch {
      case t: Throwable =>
        Logger.logger.error(s"unable to get the contrib timestamp for $data", t)
        None
    }
  }
}

object ContribTimestamp {

  /**
   * Name of contrib timestamp cookie.
   */
  val contribTimestampCookie = "gu.contributions.contrib-timestamp"

  /**
   * Set the contrib timestamp cookie if it can be inferred from the data.
   * The cookie is a way of determining whether a user has previously contributed, without using an API call.
   */
  def setContribTimestampCookie[A](result: Result, data: A)(implicit ce: ContribEvidence[A]): Result = {
    ce.contribTimestampOption(data).map { timestamp =>
      result.withCookies(Cookie(contribTimestampCookie, timestamp.toString))
    }.getOrElse(result)
  }

  trait Implicits {

    /**
     * Extend the [[Result]] class so the timestamp contrib cookie can be set via a method.
     */
    implicit class ContribResult(result: Result) {
      def setContribTimestampCookie[A : ContribEvidence](data: A): Result = {
        ContribTimestamp.setContribTimestampCookie(result, data)
      }
    }

    /**
     * Get contrib evidence from a Paypal payment.
     */
    implicit val paypalPaymentContribEvidence: ContribEvidence[Payment] = new ContribEvidence[Payment] {
      override def contribTimestamp(data: Payment): Instant = Instant.parse(data.getCreateTime)
    }

    /**
     * Get contrib evidence from a Stripe charge.
     */
    implicit val stripePaymentContribEvidence: ContribEvidence[Charge] = new ContribEvidence[Charge] {
      override def contribTimestamp(data: Charge): Instant = Instant.ofEpochSecond(data.created)
    }
  }
}
