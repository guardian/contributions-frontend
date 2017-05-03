package abtests

import actions.CommonActions.ABTestRequest
import com.github.slugify.Slugify
import play.api.Logger
import play.api.db.Database
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.AnyContent
import play.api.mvc.{Cookie, Request}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.matching.Regex

case class Percentage(value: Double) {
  def of[B](x: B)(implicit numB: Numeric[B]): Double = numB.toDouble(x) / 100 * value
}

case class Test(name: String, audienceSize: Percentage, audienceOffset: Percentage, variants: Seq[Variant], canRun: Request[_] => Boolean = _ => true) {
  val startId: Int = audienceOffset.of(Test.maxTestId).toInt
  val endId: Int = startId + audienceSize.of(Test.maxTestId).toInt
  val idRange: Range = startId.to(endId).tail

  val slug: String = Test.slugify(name)
  val cookieName: String = s"${Test.cookiePrefix}.${slug}"

  def allocate(id: Int, request: Request[_]): Option[Allocation] = {
    if (idRange.contains(id) && canRun(request)) Some(Allocation(this, variants.toList(id % variants.size)))
    else None
  }
}

object Test {
  implicit class PercentageNum[A](x: A)(implicit num: Numeric[A]) {
    val percent = Percentage(num.toDouble(x))
  }

  private val slugifier = new Slugify()

  val maxTestId: Int = 10000
  val cookiePrefix = "gu.contributions.ab"
  val testIdCookieName: String = s"$cookiePrefix.id"

  def cmpCheck(pattern: Regex)(r: Request[_]): Boolean =
    r.getQueryString("INTCMP").exists(pattern.findAllIn(_).nonEmpty)

  val stripeTest = Test("Stripe checkout", 100.percent, 0.percent, Seq(Variant("stripe")))

  val landingPageTest = Test("Landing page", 100.percent, 0.percent, Seq(Variant("with-copy")), cmpCheck("cont_.*_banner".r))

  object HumaniseTestV2 {
    import Variants._

    object Variants {
      val control = Variant("control")
      val contributionCount = Variant("contributionCount")
    }

    val test = Test(
      name = "Humanise Test V2",
      audienceSize = 100.percent,
      audienceOffset = 0.percent,
      variants = Seq(control, contributionCount)
    )

    final case class VariantData(variant: Option[Variant], contributionCount: Option[Int])

    object VariantData {
      def empty = VariantData(variant = None, contributionCount = None)
    }

    // Provides the humanise-test-v2 variant a reader is in,
    // and in addition the number of contributions,
    // should they be in the contribution count variant.
    trait TestDataProvider {

      protected[this] def getContributionsInLast7Days: Future[Int]

      def getVariantData(request: ABTestRequest[AnyContent])(implicit ec: ExecutionContext): Future[VariantData] = {
        val variant = request.getVariant(test)
        val isContributionCountVariant = variant.contains(Variants.contributionCount)

        if (!isContributionCountVariant) {
          Future.successful(VariantData(variant, contributionCount = None))
        } else {
          getContributionsInLast7Days
            .map(count => VariantData(variant, Some(count)))
            .recover {
              // If the reader is in the contribution variant,
              // and we aren't able to get the number of contributions,
              // don't put them in a variant for this test.
              case NonFatal(error) =>
                Logger.error("error getting contributions for the last 7 days", error)
                VariantData.empty
            }
        }
      }
    }

    class PostgreDataProvider(db: Database)(implicit ec: ExecutionContext) extends TestDataProvider {
      import anorm._

      // Use payment_hooks instead of all_payments due to access rights.
      private val query = SQL"""
        SELECT count(1)
        FROM payment_hooks
        WHERE
          status = 'Paid' AND
          created::date >= current_date - INTERVAL '6 day'
      """

      private val scalarIntParser = SqlParser.scalar[Int].single

      override def getContributionsInLast7Days: Future[Int] =
        Future {
          db.withConnection(autocommit = true) { implicit conn =>
            query.as(scalarIntParser)
          }
        }
    }
  }

  val allTests: Set[Test] = Set(stripeTest, landingPageTest, HumaniseTestV2.test)

  def slugify(s: String): String = slugifier.slugify(s)
  def idCookie(id: Int) = Cookie(testIdCookieName, id.toString, maxAge = Some(604800))

  def allocations(id: Int, request: Request[_]): Set[Allocation] = {
    Allocation.force(request, allTests) match {
      case Some(forced) => Set(forced)
      case None => allTests.flatMap(_.allocate(id, request))
    }
  }
}

case class Variant(name: String) {
  val slug = Test.slugify(name)
}

case class Allocation(test: Test, variant: Variant)

object Allocation {
  def force(request: Request[_], tests: Set[Test]): Option[Allocation] = for {
    params <- request.getQueryString("ab")
    allocation = params.split('=')
    testName <- allocation.lift(0)
    variantName <- allocation.lift(1)
    test <- tests.find(t => t.name.equalsIgnoreCase(testName) || t.slug.equalsIgnoreCase(testName))
    variant <- test.variants.find(v => v.name.equalsIgnoreCase(variantName) || v.slug.equalsIgnoreCase(variantName))
  } yield Allocation(test, variant)

  implicit val aw = new Writes[Allocation] {
    override def writes(a: Allocation): JsValue = Json.obj(
      "testName" -> a.test.name,
      "testSlug" -> a.test.slug,
      "variantName" -> a.variant.name,
      "variantSlug" -> a.variant.slug
    )
  }
}
