package utils

import play.api.mvc.Cookie
import play.api.mvc.Result

object ContribTimestamp {

  val contribTimestampCookie = "gu.contributions.contrib-timestamp"

  /**
   * Cookie set is a way of determining whether a user has previously contributed,
   * without using a call to an API.
   */
  def setContribTimestampCookie(result: Result, timestamp: String): Result = {
    result.withCookies(Cookie(contribTimestampCookie, timestamp))
  }
}
