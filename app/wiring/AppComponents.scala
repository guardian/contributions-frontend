package wiring

import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeCredentials, StripeService}
import play.api.BuiltInComponents
import play.api.libs.concurrent.Execution.Implicits._
import com.typesafe.config.ConfigFactory
import com.softwaremill.macwire._
import controllers._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import wiring.AppComponents.Stage
import router.Routes

//Sometimes intellij deletes this -> (import router.Routes)

class AppComponents(private val stage: Stage, c: BuiltInComponents with AhcWSComponents) {

  import c._

  lazy val metrics = new ServiceMetrics(stage.name, "giraffe","stripe")

  lazy val config = ConfigFactory.load().getConfig(s"touchpoint.backend.environments.${stage.name}")

  lazy val stripeApiConfig = StripeApiConfig.from(config, stage.name, "giraffe")
  lazy val stripeService = wire[StripeService]
  lazy val giraffeController = wire[Giraffe]
  lazy val healthcheckController = wire[Healthcheck]
  lazy val assetController = wire[Assets]


  val prefix: String = "/"
  lazy val router: Router = wire[Routes]

}

object AppComponents {

  sealed trait Stage {
    def name: String
  }

  case object DEV extends Stage {
    override def name = "DEV"
  }

  case object UAT extends Stage {
    override def name = "UAT"
  }

  case object PROD extends Stage {
    override def name = "PROD"
  }

}
