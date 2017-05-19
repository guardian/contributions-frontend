package monitoring

import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.{Logger, LoggerContext}
import com.getsentry.raven.RavenFactory
import com.getsentry.raven.dsn.Dsn
import com.getsentry.raven.logback.SentryAppender
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

// Don't have this object extend the SentryLogging trait.
// There is no contextual information at this stage of the application's life-cycle.
object SentryLogging extends LazyLogging {

  val UserIdentityId = "userIdentityId"
  val UserGoogleId = "userGoogleId"
  val PlayErrorId = "playErrorId"
  val AllMDCTags = Seq(UserIdentityId, UserGoogleId,PlayErrorId)

  def init(config: com.typesafe.config.Config) {
    Try(new Dsn(config.getString("sentry.dsn"))) match {
      case Failure(ex) =>
        logger.warn("No server-side Sentry logging configured (OK for dev)")
      case Success(dsn) =>
        logger.info(s"Initialising Sentry logging for ${dsn.getHost}")
        val buildInfo: Map[String, Any] = app.BuildInfo.toMap
        val tags = Map("stage" -> Config.stage) ++ buildInfo
        val tagsString = tags.map { case (key, value) => s"$key:$value"}.mkString(",")

        val filter = new ThresholdFilter { setLevel("ERROR") }
        filter.start() // OMG WHY IS THIS NECESSARY LOGBACK?

        val sentryAppender = new SentryAppender(RavenFactory.ravenInstance(dsn)) {
          addFilter(filter)
          setTags(tagsString)
          setRelease(app.BuildInfo.gitCommitId)
          setExtraTags((AllMDCTags ++ LoggingTag.names).mkString(","))
          setContext(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext])
        }
        sentryAppender.start()
        LoggerFactory.getLogger(ROOT_LOGGER_NAME).asInstanceOf[Logger].addAppender(sentryAppender)
    }
  }
}
