package cookies

import play.api.mvc.Result

package object syntax {

  /**
   * Allows for the following syntax: `result.setCookie[C](data)`
   */
  implicit class CookieRequest(val result: Result) extends AnyVal {
    def setCookie[C <: CookieAttributes](data: String)(implicit c: C): Result = {
      result.withCookies(Cookies.createCookie[C](data))
    }
  }
}
