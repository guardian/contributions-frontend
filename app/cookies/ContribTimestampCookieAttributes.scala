package cookies

import configuration.Config

/**
 * Should be set when a user contributes to the Guardian.
 */
trait ContribTimestampCookieAttributes extends CookieAttributes {

  override val name: String = "gu.contributions.contrib-timestamp"

  override val maxAge: Option[Int] = Some(Cookies.MaxAge.`10years`)

  /**
   * If in production, the guardian domain is used, so that the cookie will be accessible from
   * e.g. theguardian.com, contribute.theguardian.com
   */
  override val domain: Option[String] = if (Config.stageProd) Some(Config.guardianDomain) else Config.domain
}

object ContribTimestampCookieAttributes {
  implicit object contribTimestampCookieAttributes extends ContribTimestampCookieAttributes
}
