package actions

import com.gu.memsub.Subscription.{FreeMembershipSub, PaidMembershipSub}
import com.gu.memsub.util.Timing
import services._
import com.gu.memsub.{Status => SubStatus, Subscription => Sub, _}
import com.gu.monitoring.CloudWatch
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import controllers.IdentityRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._

import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.scalaFuture._


/**
 * These ActionFunctions serve as components that can be composed to build the
 * larger, more-generally useful pipelines in 'CommonActions'.
 *
 * https://www.playframework.com/documentation/2.3.x/ScalaActionsComposition
 */
object ActionRefiners extends LazyLogging {


}
