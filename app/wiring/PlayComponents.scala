package wiring

import akka.actor.ActorSystem
import play.api.BuiltInComponents
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

trait PlayComponents extends BuiltInComponents with AhcWSComponents {
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  implicit val as: ActorSystem = actorSystem
}

