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
import views.support.MessageCopyTest.CopyVariantData
import views.support.PaymentMethodTest.PaymentMethodVariantData

sealed trait VariantData

object VariantData {
  implicit val formatter: Writes[VariantData] = new Writes[VariantData] {
    def writes(o: VariantData): JsValue = {
      o match {
        case amount: AmountVariantData => AmountVariantData.format.writes(amount)
        case copy: CopyVariantData => CopyVariantData.format.writes(copy)
        case paymentMethods: PaymentMethodVariantData => PaymentMethodVariantData.format.writes(paymentMethods)

      }
    }
  }
}

case class Variant(testName: String, testSlug: String, variantName: String, variantSlug: String, weight: Double, data: VariantData, countryGroupFilter: Set[CountryGroup] = Set.empty) {
  def matches(countryGroup: CountryGroup): Boolean = countryGroupFilter.isEmpty || countryGroupFilter.contains(countryGroup)
}

object Variant {
  implicit val writes: Writes[Variant] = (
    (__ \ "testName").write[String] and
      (__ \ "testSlug").write[String] and
      (__ \ "variantName").write[String] and
      (__ \ "variantSlug").write[String] and
      (__ \ "weight").write[Double] and
      (__ \ "data").write[VariantData]
    )(v => (v.testName, v.testSlug, v.variantName, v.variantSlug, v.weight, v.data))
}

trait TestTrait {
  def name: String
  def slug: String
  def variants: NonEmptyList[Variant]

  def makeVariant(variantName: String, variantSlug: String, weight: Double, data: VariantData, countryGroupFilter: Set[CountryGroup] = Set.empty): Variant = {
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
    makeVariant("Amount - 25 highlight", "25", 0, AmountVariantData(List(25, 50, 100, 250), Some(25)), notAustralia),
    makeVariant("Amount - 50 highlight", "50", 1, AmountVariantData(List(25, 50, 100, 250), Some(50)), notAustralia),
    makeVariant("Amount - 100 highlight", "100", 0, AmountVariantData(List(25, 50, 100, 250), Some(100)), notAustralia),
    makeVariant("Amount - 250 highlight", "250", 0, AmountVariantData(List(25, 50, 100, 250), Some(250)), notAustralia),
    makeVariant("Amount - 100 highlight Australia", "100-Australia", 1, AmountVariantData(List(50, 100, 250, 500), Some(100)), Set(Australia))
  )
}

object MessageCopyTest extends TestTrait {
  def name = "MessageCopyTest"
  def slug = "mcopy"

  case class CopyVariantData(message: String) extends VariantData

  object CopyVariantData {
    implicit val format: Format[CopyVariantData] = Json.format[CopyVariantData]
  }

  def variants = NonEmptyList(
    makeVariant("Copy - control", "control", 1, CopyVariantData("Support the Guardian")),
    makeVariant("Copy - support", "support", 0, CopyVariantData("Support the Guardian")),
    makeVariant("Copy - power", "power", 0, CopyVariantData("The powerful won't investigate themselves. That's why we need you.")),
    makeVariant("Copy - mutual", "mutual", 0, CopyVariantData("Can't live without us? The feeling's mutual.")),
    makeVariant("Copy - everyone", "everyone", 0, CopyVariantData("If everyone who sees this chipped in the Guardian's future would be more secure.")),
    makeVariant("Copy - everyone", "everyoneinline", 0, CopyVariantData("If everyone who sees this chipped in the Guardian's future would be more secure.")),
    makeVariant("Copy - everyone editorial", "everyone-editorial", 0, CopyVariantData("If everyone who sees this chipped in the Guardian's future would be more secure.")),
    makeVariant("Copy - expensive", "expensive", 0, CopyVariantData("Producing the Guardian is expensive. Supporting it isn't.")),
    makeVariant("Copy - expensive inline", "expensiveinline", 0, CopyVariantData("Producing the Guardian is expensive. Supporting it isn't.")),
    makeVariant("Copy - british", "british", 0, CopyVariantData("It's not very British to talk about money. So we'll just ask for it instead.")),
    makeVariant("Copy - british inline", "britishinline", 0, CopyVariantData("It's not very British to talk about money. So we'll just ask for it instead.")),
    makeVariant("Copy - powerless", "powerless", 0, CopyVariantData("Don't let the powerless pay the price. Make your contribution")),
    makeVariant("Copy - powerless inline", "powerlessinline", 0, CopyVariantData("Don't let the powerless pay the price. Make your contribution")),
    makeVariant("Copy - coffee inline", "costofnewswithyourcoffeeinline", 0, CopyVariantData("Do you want the news with your coffee or do you just want coffee? Quality journalism costs. Please contribute.")),
    makeVariant("Copy - coffee", "costofnewswithyourcoffee", 0, CopyVariantData("Do you want the news with your coffee or do you just want coffee? Quality journalism costs. Please contribute.")),
    makeVariant("Copy - heritage inline", "heritageinline", 0, CopyVariantData("From the Peterloo massacre to phone hacking and the Panama Papers, we've been there - on your side for almost 200 years. Contribute to the Guardian today")),
    makeVariant("Copy - heritage", "heritage", 0, CopyVariantData("From the Peterloo massacre to phone hacking and the Panama Papers, we've been there - on your side for almost 200 years. Contribute to the Guardian today")),
    makeVariant("Copy - global beijing inline", "global-beijing-inline", 0, CopyVariantData("By the time you've had your morning tea, reporters in Rio, Beijing, Moscow, Berlin, Paris, Johannesburg have already filed their stories. Covering the world's news isn't cheap. Please chip in a few pounds.")),
    makeVariant("Copy - global beijing", "global-beijing", 0, CopyVariantData("By the time you've had your morning tea, reporters in Rio, Beijing, Moscow, Berlin, Paris, Johannesburg have already filed their stories. Covering the world's news isn't cheap. Please chip in a few pounds."))
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
    makeVariant("Control", "control", 0.5, PaymentMethodVariantData(Set(CARD))),
    makeVariant("Paypal", "paypal", 0.5, PaymentMethodVariantData(Set(CARD, PAYPAL)))
  )

}
case class ChosenVariants(variants: Seq[Variant]) {
  def asJson = Json.toJson(variants)
  def encodeURL = URLEncoder.encode(asJson.toString, StandardCharsets.UTF_8.name())
}

object Test {

  val allTests = List(AmountHighlightTest, MessageCopyTest)

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
    ChosenVariants(Seq(pickVariant(countryGroup, request, AmountHighlightTest), pickVariant(countryGroup, request, MessageCopyTest), pickVariant(countryGroup, request, PaymentMethodTest)))
  }
}




