package cookies

import configuration.Config

/**
 * Should be set when a user contributes to the Guardian.
 */
trait GuEmailCookieAttributes extends CookieAttributes {

  override val name: String = "gu.email"

  override val maxAge: Option[Int] = None

  /**
   * If in production, the guardian domain is used, so that the cookie will be accessible from
   * e.g. theguardian.com, contribute.theguardian.com
   */
  override val domain: Option[String] = if (Config.stageProd) Some(Config.guardianDomain) else Config.domain.map(_.stripPrefix("contribute"))
}

object GuEmailCookieAttributes {
  implicit object guEmailCookieAttributes extends GuEmailCookieAttributes
}

