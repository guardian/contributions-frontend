package monitoring

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MDC
import play.api.mvc.Request

// Tags to be included in a log statement using Mapped Diagnostic Context (MDC).
// A MDC can be used to supplement log messages with additional contextual information,
// or provide tags to a Sentry Appender.
//
// References:
// - https://logback.qos.ch/manual/mdc.html, and
// - https://github.com/getsentry/raven-java/blob/master/raven-logback/src/main/java/com/getsentry/raven/logback/SentryAppender.java
case class LoggingTags(browserId: String, requestId: UUID) {

  def toMap: Map[String, String] = Map(
    LoggingTags.browserId -> browserId,
    LoggingTags.requestId -> requestId.toString
  )
}

object LoggingTags {

  val browserId = "browserId"
  val requestId = "requestId"

  val allTags = Seq(browserId, requestId)

  // Means that if `implicit request =>` is used in an Action, an implicit SentryLoggingTags instance will be in scope.
  implicit def fromRequest(implicit request: Request[Any]): LoggingTags = {
    val browserId = request.cookies.get("bwid").map(_.value).getOrElse("unknown")
    LoggingTags(browserId, UUID.randomUUID)
  }
}

trait TagAwareLogger extends LazyLogging {

  private[this] def withTags(loggingExpr: => Unit)(implicit tags: LoggingTags): Unit =
    try {
      for ((tagName, tagValue) <- tags.toMap) MDC.put(tagName, tagValue)
      loggingExpr
    } finally {
      MDC.clear()
    }

  def info(msg: String)(implicit tags: LoggingTags): Unit =
    withTags(logger.info(msg))

  def error(msg: String, t: Throwable)(implicit tags: LoggingTags): Unit =
    withTags(logger.error(msg, t))

  def error(msg: String)(implicit tags: LoggingTags): Unit =
    withTags(logger.error(msg))
}
