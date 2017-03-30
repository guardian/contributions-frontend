package abtests

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.{BooleanOperators, forAll}
import org.scalacheck.{Arbitrary, Gen, Properties}
import play.api.mvc.Request
import play.api.test.FakeRequest

object ABTestSpec extends Properties("test") {
  val testIdGen = Gen.posNum[Int].suchThat(i => i > 0 && i <= Test.maxTestId)
  val fakeRequest: Request[_] = FakeRequest()

  implicit val ap: Arbitrary[Percentage] = Arbitrary(Gen.posNum[Double].suchThat(_ <= 100).map(Percentage))
  implicit val av: Arbitrary[Variant] = Arbitrary(Gen.alphaStr.map(Variant))

  implicit val at: Arbitrary[Test] = Arbitrary(for {
    name <- Gen.alphaStr
    audienceSize <- arbitrary[Percentage]
    audienceOffset <- arbitrary[Percentage]
    variants <- Gen.nonEmptyContainerOf[Seq, Variant](arbitrary[Variant])
    canRun <- arbitrary[Boolean]
  } yield Test(name, audienceSize, audienceOffset, variants, _ => canRun))

  property("test allocation covers IDs and canRun conditions correctly") = forAll { (test: Test) =>
    test.idRange.forall(id => test.allocate(id, fakeRequest).nonEmpty == test.canRun(fakeRequest))
  }

  property("test allocation assigns all variants") = forAll { (test: Test) =>
    val allocatedVariants = test.idRange.flatMap(id => test.allocate(id, fakeRequest)).map(_.variant).toSet

    test.canRun(fakeRequest) ==> (allocatedVariants == test.variants.toSet) ||
      !test.canRun(fakeRequest) ==> (allocatedVariants == Set.empty)
  }
}
