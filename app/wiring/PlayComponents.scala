package wiring

import akka.actor.ActorSystem
import play.api.BuiltInComponents
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.libs.ws.ahc.AhcWSComponents
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
}

