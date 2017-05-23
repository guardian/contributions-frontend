package monitoring

import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MDC
import play.api.mvc.RequestHeader

// Tags to be included in a log statement using Mapped Diagnostic Context (MDC).
// A MDC can be used to e.g. supplement log messages with additional contextual information,
// or provide tags to a Sentry Appender.
//
// References:
// - https://logback.qos.ch/manual/mdc.html
// - https://github.com/getsentry/raven-java/blob/master/raven-logback/src/main/java/com/getsentry/raven/logback/SentryAppender.java
sealed trait LoggingTag extends enumeratum.EnumEntry { self =>

  def name: String = self.entryName

  // The value of each tag should be able to be derived from a request header.
  def value(header: RequestHeader): String
}

object LoggingTag extends enumeratum.Enum[LoggingTag] {

  override def values: Seq[LoggingTag] = findValues

  def names: Seq[String] = values.map(_.name)

  case object BrowserId extends LoggingTag {
    override def value(header: RequestHeader): String =
      header.cookies.get("bwid").map(_.value).getOrElse("unknown")
  }

  case object RequestId extends LoggingTag {
    override def value(header: RequestHeader): String = header.id.toString
  }

  case object Path extends LoggingTag {
    override def value(header: RequestHeader): String = header.path
  }
}

// Constructor private as a LoggingTags instance should only ever be created from the fromRequestHeader() method.
case class LoggingTags private (tags: Map[TagKey, TagValue])

object LoggingTags {

  // Means that if `implicit request =>` is used in an Action, an implicit LoggingTags instance will be in scope.
  implicit def fromRequestHeader(implicit header: RequestHeader): LoggingTags =
    LoggingTags {
      LoggingTag.values.foldLeft(Map.empty[TagKey, TagValue]) {
        case (tags, tag) => tags + (tag.name -> tag.value(header))
      }
    }
}

trait TagAwareLogger extends LazyLogging {

  private[this] def withTags(loggingExpr: => Unit)(implicit tags: LoggingTags): Unit =
    try {
      for ((tagName, tagValue) <- tags.tags) MDC.put(tagName, tagValue)
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
