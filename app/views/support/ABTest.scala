package views.support

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Cookie, Request}


import scala.util.Random
import scalaz.NonEmptyList
import views.support.AmountHighlightTest.AmountVariantData
import views.support.PaymentMethodTest.PaymentMethodVariantData

sealed trait VariantData

object VariantData {
  implicit val formatter: Writes[VariantData] = new Writes[VariantData] {
    def writes(o: VariantData): JsValue = {
      o match {
        case amount: AmountVariantData => AmountVariantData.format.writes(amount)
        case paymentMethods: PaymentMethodVariantData => PaymentMethodVariantData.format.writes(paymentMethods)
      }
    }
  }
}

case class Variant(testName: String, testSlug: String, variantName: String, variantSlug: String, weight: Double, data: Option[VariantData], countryGroupFilter: Set[CountryGroup] = Set.empty) {
  def matches(countryGroup: CountryGroup): Boolean = countryGroupFilter.isEmpty || countryGroupFilter.contains(countryGroup)
}

object Variant {
  implicit val writes: Writes[Variant] = (
    (__ \ "testName").write[String] and
      (__ \ "testSlug").write[String] and
      (__ \ "variantName").write[String] and
      (__ \ "variantSlug").write[String] and
      (__ \ "weight").write[Double] and
      (__ \ "data").writeNullable[VariantData]
    )(v => (v.testName, v.testSlug, v.variantName, v.variantSlug, v.weight, v.data))
}

trait TestTrait {
  def name: String
  def slug: String
  def variants: NonEmptyList[Variant]

  def makeVariant(variantName: String, variantSlug: String, weight: Double, data: Option[VariantData] = None, countryGroupFilter: Set[CountryGroup] = Set.empty): Variant = {
    Variant(name, slug, variantName, variantSlug, weight, data, countryGroupFilter)
  }

 val variantsByCountry: Map[CountryGroup, Set[Variant]] = {

    def filterVariants(countryGroup: CountryGroup) = variants.list.filter(_.matches(countryGroup)).toSet

    CountryGroup.allGroups.map(country => country -> filterVariants(country)).toMap
    }

  val variantRangesByCountry: Map[CountryGroup, Seq[(Double, Variant)]] = {

   def addRanges(filteredVariants: Set[Variant]): Seq[(Double, Variant)] = {
     val filteredVariantList = filteredVariants.toList
     val variantWeights = filteredVariantList.map(_.weight)
     val weightSum = variantWeights.sum
     val totalWeight = if (weightSum != 0.0) weightSum else 1
     val cdf: Seq[Double] = variantWeights.scanLeft(0.0)(_ + _).tail.map(_ / totalWeight)
     cdf.zip(filteredVariantList)
   }

    variantsByCountry.map { case (country, variants) => country -> addRanges(variants)}
  }
}

object AmountHighlightTest extends TestTrait {
  def name = "AmountHighlightTest"
  def slug = "highlight"

  case class AmountVariantData(values: List[Int], preselect: Option[Int]) extends VariantData

  object AmountVariantData {
    implicit val format: Format[AmountVariantData] = Json.format[AmountVariantData]
  }

  private lazy val notAustralia: Set[CountryGroup] = Set(
    UK,
    US,
    Canada,
    NewZealand,
    Europe,
    RestOfTheWorld
  )

  def variants = NonEmptyList(
    //New variants go here.
    makeVariant("Amount - 25 highlight", "25", 1, Some(AmountVariantData(List(25, 50, 100, 250), Some(25))), notAustralia),
    makeVariant("Amount - 50 highlight", "50", 0, Some(AmountVariantData(List(25, 50, 100, 250), Some(50))), notAustralia),
    makeVariant("Amount - 100 highlight", "100", 0, Some(AmountVariantData(List(25, 50, 100, 250), Some(100))), notAustralia),
    makeVariant("Amount - 250 highlight", "250", 0, Some(AmountVariantData(List(25, 50, 100, 250), Some(250))), notAustralia),
    makeVariant("Amount - 100 highlight Australia", "100-Australia", 1, Some(AmountVariantData(List(50, 100, 250, 500), Some(100))), Set(Australia))
  )
}

object PaymentMethodTest extends TestTrait {

  def name = "PaymentMethodTest"

  def slug = "paymentMethods"

  case class PaymentMethodVariantData(paymentMethods: Set[PaymentMethod]) extends VariantData

  object PaymentMethodVariantData {
    implicit val format: Writes[PaymentMethodVariantData] = Json.writes[PaymentMethodVariantData]
  }

  sealed trait PaymentMethod

  case object CARD extends PaymentMethod

  case object PAYPAL extends PaymentMethod

  implicit val paymentMethodFormatter: Writes[PaymentMethod] = new Writes[PaymentMethod] {
    def writes(method: PaymentMethod): JsValue = JsString(method.toString)
  }

  def variants = NonEmptyList(
    makeVariant("Control", "control", 0.5, Some(PaymentMethodVariantData(Set(CARD)))),
    makeVariant("Paypal", "paypal", 0.5, Some(PaymentMethodVariantData(Set(CARD, PAYPAL))))
  )
}

object RecurringPaymentTest extends TestTrait {
  def name = "RecurringPaymentTest"
  def slug = "recurringPayment"

  def variants = NonEmptyList(
    makeVariant("control", "control", 0.5),
    makeVariant("recurring", "recurring", 0.5)
  )
}

case class ChosenVariants(variants: Seq[Variant]) {
  def asJson = Json.toJson(variants)
  def encodeURL = URLEncoder.encode(asJson.toString, StandardCharsets.UTF_8.name())
}

object Test {

  val allTests = List(AmountHighlightTest) // only used in unit tests

  def pickVariant[A](countryGroup: CountryGroup, request: Request[A], test: TestTrait): Variant = {

    def pickRandomly: Variant = {
      val n = Random.nextDouble
      test.variantRangesByCountry(countryGroup).dropWhile(_._1 < n).head._2
    }

    def pickByQueryStringOrCookie[A]: Option[Variant] = {
      val search: Option[String] = request.getQueryString(test.slug)
        .orElse(request.cookies.get(test.slug + "_GIRAFFE_TEST").map(_.value))
      test.variantsByCountry(countryGroup).find(_.variantSlug == search.getOrElse(None))
    }

    pickByQueryStringOrCookie getOrElse pickRandomly
  }

  def createCookie(variant: Variant): Cookie = {
    Cookie(variant.testSlug+"_GIRAFFE_TEST", variant.variantSlug, maxAge = Some(604800))
  }

  def getContributePageVariants[A](countryGroup: CountryGroup,request: Request[A]) = {
    ChosenVariants(Seq(
      pickVariant(countryGroup, request, AmountHighlightTest),
      pickVariant(countryGroup, request, PaymentMethodTest),
      pickVariant(countryGroup, request, RecurringPaymentTest)
    ))
  }
}




