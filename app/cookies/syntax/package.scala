package cookies

import play.api.mvc.Result

package object syntax {

  /**
   * Allows for the following syntax: `result.setCookie[C].using(data)`
   */
  implicit class CookieRequest(result: Result) {
    def setCookie[C <: CookieType] = new CookieRequest.CookieFactory[C](result)
  }

  object CookieRequest {
    class CookieFactory[C <: CookieType](result: Result) {
      def using[A](data: A)(implicit c: C, cv: CookieValue[A, C]): Result = {
        result.withCookies(CookieType.createCookie[C, A](data))
      }
    }
  }
}
