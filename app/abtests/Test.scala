package abtests

import com.github.slugify.Slugify
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Cookie

case class Percentage(value: Double) {
  def of[B](x: B)(implicit numB: Numeric[B]): Double = numB.toDouble(x) / 100 * value
}

case class Test(name: String, audienceSize: Percentage, audienceOffset: Percentage, variants: Variant*) {
  val startId: Int = audienceOffset.of(Test.maxTestId).toInt
  val endId: Int = startId + audienceSize.of(Test.maxTestId).toInt
  val idRange: Range = startId.to(endId).tail

  val slug: String = Test.slugify(name)
  val cookieName: String = s"${Test.cookiePrefix}.${slug}"

  def allocate(id: Int): Option[Allocation] = {
    if (!idRange.contains(id)) None
    else Some(Allocation(this, variants.toList(id % variants.size)))
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

  val stripeTest = Test("Stripe checkout", 30.percent, 0.percent, Variant("control"), Variant("stripe"))
  val allTests: Set[Test] = Set(stripeTest)

  def slugify(s: String): String = slugifier.slugify(s)
  def idCookie(id: Int) = Cookie(testIdCookieName, id.toString, maxAge = Some(604800))
  def allocations(id: Int): Set[Allocation] = allTests.flatMap(_.allocate(id))
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
