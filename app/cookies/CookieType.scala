package cookies

import play.api.mvc.Cookie

abstract class CookieType {
  val name: String
  val maxAge: Option[Int] = None
  val path: String = "/"
  val domain: Option[String] = None
  val secure: Boolean = false
  val httpOnly: Boolean = false
}

/**
 * Represents data of type [[A]] being able to set the value for a given cookie type [[C]].
 */
trait CookieValue[A, C <: CookieType] {
  def value(data: A): String
}

object CookieType {

  /**
   * Create a cookie from a cookie type, using the data to set its value.
   */
  def createCookie[C <: CookieType, A](data: A)(implicit c: C, cv: CookieValue[A, C]): Cookie = {
    Cookie(c.name, cv.value(data), c.maxAge, c.path, c.domain, c.secure, c.httpOnly)
  }
}

object Cookies {

  /**
   * Common max ages for cookies.
   */
  object MaxAge {

    val `10years` = 60 * 60 * 24 * 365 * 10
  }
}
