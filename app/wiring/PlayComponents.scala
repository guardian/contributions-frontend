package wiring

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.BuiltInComponents
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.libs.ws.ahc.AhcWSComponents
import play.filters.csrf.CSRFAddToken
import play.filters.csrf.CSRFCheck
import play.filters.csrf.CSRFComponents

import scala.concurrent.ExecutionContext

trait PlayComponents extends BuiltInComponents
  with AhcWSComponents
  with DBComponents
  with HikariCPComponents
  with CSRFComponents {

  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  implicit val as: ActorSystem = actorSystem

  val jdbcExecutionContext: ExecutionContext = actorSystem.dispatchers.lookup("contexts.jdbc-context")

  val csrfAddToken = CSRFAddToken(csrfConfig, csrfTokenSigner)
  val csrfCheck = CSRFCheck(csrfConfig, csrfTokenSigner)
}

