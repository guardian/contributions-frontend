package monitoring

import java.util.UUID

import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.{Logger, LoggerContext}
import com.getsentry.raven.RavenFactory
import com.getsentry.raven.dsn.Dsn
import com.getsentry.raven.logback.SentryAppender
import configuration.Config
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import play.api.mvc.Request

import scala.util.{Failure, Success, Try}

object SentryLogging {

  def init(config: com.typesafe.config.Config) {
    Try(new Dsn(config.getString("sentry.dsn"))) match {
      case Failure(ex) =>
        play.api.Logger.warn("No server-side Sentry logging configured (OK for dev)")
      case Success(dsn) =>
        play.api.Logger.info(s"Initialising Sentry logging for ${dsn.getHost}")
        val buildInfo: Map[String, Any] = app.BuildInfo.toMap
        val tags = Map("stage" -> Config.stage) ++ buildInfo
        val tagsString = tags.map { case (key, value) => s"$key:$value"}.mkString(",")

        val filter = new ThresholdFilter { setLevel("ERROR") }
        filter.start() // OMG WHY IS THIS NECESSARY LOGBACK?

        val sentryAppender = new SentryAppender(RavenFactory.ravenInstance(dsn)) {
          addFilter(filter)
          setTags(tagsString)
          setRelease(app.BuildInfo.gitCommitId)
          setExtraTags(SentryLoggingTags.AllTags.mkString(","))
          setContext(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext])
        }
        sentryAppender.start()
        LoggerFactory.getLogger(ROOT_LOGGER_NAME).asInstanceOf[Logger].addAppender(sentryAppender)
    }
  }
}


case class SentryLoggingTags(browserId: String, requestId: UUID) {

  def toMap: Map[String, String] = Map(
    SentryLoggingTags.browserId -> browserId,
    SentryLoggingTags.requestId -> requestId.toString
  )
}

object SentryLoggingTags {

  val userIdentityId = "userIdentityId"
  val userGoogleId = "userGoogleId"
  val playErrorId = "playErrorId"
  val browserId = "browserId"
  val requestId = "requestId"

  val AllTags = Seq(userIdentityId, userGoogleId, playErrorId, browserId, requestId)

  // Means that if `implicit request =>` is used in an Action, an implicit SentryLoggingTags instance will be in scope.
  implicit def fromRequest(implicit request: Request[Any]): SentryLoggingTags = {
    val browserId = request.cookies.get("bwid").map(_.value).getOrElse("unknown")
    SentryLoggingTags(browserId, UUID.randomUUID)
  }
}

class SentryTagLogger private(logger: org.slf4j.Logger) {

  def withTags(expr: => Unit)(implicit tags: SentryLoggingTags): Unit = {
    try {
      for ((k, v) <- tags.toMap) MDC.put(k, v)
      expr
    } finally {
      MDC.clear()
    }
  }

  def info(msg: String)(implicit tags: SentryLoggingTags): Unit =
    withTags(logger.info(msg))

  def error(msg: String, t: Throwable)(implicit tags: SentryLoggingTags): Unit =
    withTags(logger.error(msg, t))

  def error(msg: String)(implicit tags: SentryLoggingTags): Unit =
    withTags(logger.error(msg))
}

object SentryTagLogger extends SentryTagLogger(play.api.Logger.logger)
