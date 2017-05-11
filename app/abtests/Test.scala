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

  val landingPageTest = Test("Landing page", 100.percent, 0.percent, Seq(Variant("with-copy")), cmpCheck("gdnwb_copts.*_banner".r))

  val allTests: Set[Test] = Set(stripeTest, landingPageTest)

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
