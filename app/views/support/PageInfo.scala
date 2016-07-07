package views.support

import com.gu.i18n.{Country, CountryGroup, Currency, GBP}
import com.gu.memsub.BillingPeriod
import play.api.libs.json._
import scalaz.syntax.std.option._

case class PageInfo(title: String = "toot toot",
                    url: String = "/",
                    description: Option[String] = "toot toot".some,
                    image: Option[String] = "picture.jpg".some,
                    customSignInUrl: Option[String] = None,
                    stripePublicKey: Option[String] = None
                   )
