package utils

import org.joda.time.{DateTimeZone, Years, DateTime}
import play.api.mvc.Result

object RichResults {
  private final val DateTimePattern = "EEE, dd MMM yyyy HH:mm:ss 'GMT"
  private def dateToString(dt: DateTime) = dt.withZone(DateTimeZone.forID("GMT")).toString(DateTimePattern)

  implicit class RichResult(val result: Result) extends AnyVal {
    def withoutCache: Result = result.withHeaders(
      "Cache-Control" -> "no-cache, private, no-store, must-revalidate",
      "Pragma" -> "no-cache",
      "Expires" -> dateToString(DateTime.now minus Years.ONE)
    )
  }
}
