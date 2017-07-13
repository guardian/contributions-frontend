package monitoring

import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MDC
import play.api.mvc.RequestHeader
import utils.Hasher
import utils.SHA256Hasher

// Tags to be included in a log statement using Mapped Diagnostic Context (MDC).
// A MDC can be used to e.g. supplement log messages with additional contextual information,
// or provide tags to a Sentry Appender.
//
// References:
// - https://logback.qos.ch/manual/mdc.html
// - https://github.com/getsentry/raven-java/blob/master/raven-logback/src/main/java/com/getsentry/raven/logback/SentryAppender.java
sealed trait LoggingTag extends enumeratum.EnumEntry { self =>

  def name: String = self.entryName

  def hashValue: Boolean

  def value(header: RequestHeader): Option[String]

  def loggingValue(header: RequestHeader, hasher: Hasher): String =
    value(header).map { tagValue => if (hashValue) hasher.hash(tagValue) else tagValue }.getOrElse("unknown")
}

object LoggingTag extends enumeratum.Enum[LoggingTag] {

  override def values: Seq[LoggingTag] = findValues

  def names: Seq[String] = values.map(_.name)

  case object BrowserId extends LoggingTag {
    override val hashValue: Boolean = true
    override def value(header: RequestHeader): Option[String] =
      header.cookies.get("bwid").map(_.value)
  }

  case object RequestId extends LoggingTag {
    override val hashValue: Boolean = false
    override def value(header: RequestHeader): Option[String] = Some(header.id.toString)
  }

  case object Path extends LoggingTag {
    override val hashValue: Boolean = false
    override def value(header: RequestHeader): Option[String] = Some(header.path)
  }
}

// Constructor private as a LoggingTags instance should only ever be created from the fromRequestHeader() method.
case class LoggingTags private (tags: Map[TagKey, TagValue])

// Mixing this into a controller means that when `implicit request =>` is used in an Action,
// an implicit LoggingTags instance for the request will be in scope.
// This logic could be handled in the LoggingTags companion object,
// but it's maybe better to require an explicit mixin to get this implicit behaviour (?)
trait LoggingTagsProvider {

  // InfoSec have said it is ok to use the SHA-256 algorithm for hashing sensitive data.
  private val hasher: Hasher = SHA256Hasher

  implicit def loggingTagsFromRequestHeader(implicit header: RequestHeader): LoggingTags =
    LoggingTags {
      LoggingTag.values.foldLeft(Map.empty[TagKey, TagValue]) {
        case (tags, tag) => tags + (tag.name -> tag.loggingValue(header, hasher))
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

  def warn(msg: String)(implicit tags: LoggingTags): Unit =
    withTags(logger.warn(msg))

  def error(msg: String, t: Throwable)(implicit tags: LoggingTags): Unit =
    withTags(logger.error(msg, t))

  def error(msg: String)(implicit tags: LoggingTags): Unit =
    withTags(logger.error(msg))
}
