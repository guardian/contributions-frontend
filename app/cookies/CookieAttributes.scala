package cookies

import play.api.mvc.Cookie

abstract class CookieAttributes {
  val name: String
  val maxAge: Option[Int] = None
  val path: String = "/"
  val domain: Option[String] = None
  val secure: Boolean = false
  val httpOnly: Boolean = false
}


object Cookies {

  /**
   * Create a cookie from a cookie type, using the data to set its value.
   */
  def createCookie[CA <: CookieAttributes](data: String)(implicit ca: CA): Cookie = {
    Cookie(ca.name, data, ca.maxAge, ca.path, ca.domain, ca.secure, ca.httpOnly)
  }

  /**
   * Common max ages for cookies.
   */
  object MaxAge {

    val `10years` = 60 * 60 * 24 * 365 * 10
  }
}
