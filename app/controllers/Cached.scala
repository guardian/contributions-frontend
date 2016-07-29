package controllers

import java.time.Duration.ofDays
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.time.{Instant, ZoneOffset}

import play.api.mvc.Result

import scala.math.max

object Cached {

  private val cacheableStatusCodes = Seq(200, 301, 404)

  private val tenDaysInSeconds = ofDays(10).getSeconds

  //http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1
  private val HTTPDateFormat =  RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

  def apply(result: Result): Result = apply(60)(result)

  def apply(seconds: Int)(result: Result): Result = {
    if (suitableForCaching(result)) cacheHeaders(seconds, result) else result
  }

  def suitableForCaching(result: Result): Boolean = cacheableStatusCodes.contains(result.header.status)

  private def cacheHeaders(maxAge: Int, result: Result) = {
    val now = Instant.now
    val staleWhileRevalidateSeconds = max(maxAge / 10, 1)
    result.withHeaders(
      "Cache-Control" -> s"public, max-age=$maxAge, stale-while-revalidate=$staleWhileRevalidateSeconds, stale-if-error=$tenDaysInSeconds",
      "Expires" -> HTTPDateFormat.format(now plusSeconds maxAge),
      "Date" -> HTTPDateFormat.format(now)
    )
  }

}

object NoCache {
  def apply(result: Result): Result = result.withHeaders("Cache-Control" -> "no-cache, private", "Pragma" -> "no-cache")
}
