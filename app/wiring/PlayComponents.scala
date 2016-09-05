package wiring

import akka.actor.ActorSystem
import play.api.BuiltInComponents
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

trait PlayComponents extends BuiltInComponents with AhcWSComponents with DBComponents with HikariCPComponents {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  implicit val as: ActorSystem = actorSystem
}

