package abtests

import com.github.slugify.Slugify
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Cookie, Request}

import scala.util.matching.Regex

case class Percentage(value: Double) {
  def of[B](x: B)(implicit numB: Numeric[B]): Double = numB.toDouble(x) / 100 * value
}

sealed trait Test {
  val name: String
  val audienceSize: Percentage
  val audienceOffset: Percentage
  val variants: Iterable[Variant]

  val startId: Int = audienceOffset.of(Test.maxTestId).toInt
  val endId: Int = startId + audienceSize.of(Test.maxTestId).toInt
  val idRange: Range = startId.to(endId).tail

  val slug: String = Test.slugify(name)
  val cookieName: String = s"${Test.cookiePrefix}.${slug}"

  def canRun(req: Request[_]): Boolean

  def allocate(id: Int, request: Request[_]): Option[Allocation] = {
    if (idRange.contains(id) && canRun(request)) Some(Allocation(this, variants.toList(id % variants.size)))
    else None
  }
}

case class SplitTest(name: String, audienceSize: Percentage, audienceOffset: Percentage, variants: Variant*) extends Test {
  override def canRun(req: Request[_]): Boolean = true
}
case class ConditionalTest(name: String, audienceSize: Percentage, audienceOffset: Percentage, canRunCheck: Request[_] => Boolean, variants: Variant*) extends Test {
  override def canRun(req: Request[_]): Boolean = canRunCheck(req)
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

  val stripeTest = SplitTest("Stripe checkout", 100.percent, 0.percent, Variant("control"), Variant("stripe"))
  val landingPageTest = ConditionalTest("Landing page", 100.percent, 0.percent, cmpCheck("mem_.*_banner".r), Variant("control"), Variant("with-copy"))
  val allTests: Set[Test] = Set(stripeTest, landingPageTest)

  def slugify(s: String): String = slugifier.slugify(s)
  def idCookie(id: Int) = Cookie(testIdCookieName, id.toString, maxAge = Some(604800))
  def allocations(id: Int, request: Request[_]): Set[Allocation] = allTests.flatMap(_.allocate(id, request))
}

case class Variant(name: String) {
  val slug = Test.slugify(name)
}

case class Allocation(test: Test, variant: Variant)

object Allocation {
  implicit val aw = new Writes[Allocation] {
    override def writes(a: Allocation): JsValue = Json.obj(
      "testName" -> a.test.name,
      "testSlug" -> a.test.slug,
      "variantName" -> a.variant.name,
      "variantSlug" -> a.variant.slug
    )
  }
}
