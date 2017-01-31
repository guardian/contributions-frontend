package abtests

import com.github.slugify._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Cookie

case class Test(name: String, audienceSize: Double, audienceOffset: Double, variants: Variant*) {
  private val startId: Int = (audienceOffset * Test.maxTestId).toInt
  private val endId: Int = (startId + (audienceSize * Test.maxTestId)).toInt
  private val idRange: Range = startId.to(endId).tail

  val slug: String = Test.slugify(name)
  val cookieName: String = s"${Test.cookiePrefix}.${slug}"

  def allocate(id: Int): Option[Allocation] = {
    idRange.grouped(idRange.length / variants.size)
      .zip(variants.iterator)
      .find { case (ids, _) => id >= ids.head && id <= ids.last }
      .map(t => Allocation(this, t._2))
  }
}

object Test {
  private val slugifier = new Slugify()

  val maxTestId = 100
  val cookiePrefix = "gu.contributions.test"
  val testIdCookieName = s"$cookiePrefix.id"

  val stripeTest = Test("Stripe checkout", 0.3, 0, Variant("control"), Variant("stripe"))
  val allTests = Set(stripeTest)

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
