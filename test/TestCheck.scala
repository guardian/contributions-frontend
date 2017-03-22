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

  implicit val at: Arbitrary[SplitTest] = Arbitrary(for {
    name <- Gen.alphaStr
    audienceSize <- arbitrary[Percentage]
    audienceOffset <- arbitrary[Percentage]
    variants <- Gen.nonEmptyContainerOf[Seq, Variant](arbitrary[Variant])
  } yield SplitTest(name, audienceSize, audienceOffset, variants: _*))

  implicit val act: Arbitrary[ConditionalTest] = Arbitrary(for {
    t <- arbitrary[SplitTest]
    canRun <- arbitrary[Boolean]
  } yield ConditionalTest(t.name, t.audienceSize, t.audienceOffset, _ => canRun, t.variants: _*))

  property("split test allocation covers IDs correctly") = forAll { (test: SplitTest) =>
    test.idRange.forall(id => test.allocate(id, fakeRequest).nonEmpty) &&
      (1 to Test.maxTestId).diff(test.idRange).forall(id => test.allocate(id, fakeRequest).isEmpty)
  }

  property("split test allocation assigns all variants") = forAll { (test: SplitTest) =>
    test.idRange.flatMap(id => test.allocate(id, fakeRequest)).map(_.variant).toSet == test.variants.toSet
  }

  property("conditional test allocation covers IDs and canRun conditions correctly") = forAll { (test: ConditionalTest) =>
    test.idRange.forall(id => test.allocate(id, fakeRequest).nonEmpty == test.canRun(fakeRequest)) &&
      (1 to Test.maxTestId).diff(test.idRange).forall(id => test.allocate(id, fakeRequest).isEmpty)
  }

  property("conditional test allocation assigns all variants") = forAll { (test: ConditionalTest) =>
    val allocatedVariants = test.idRange.flatMap(id => test.allocate(id, fakeRequest)).map(_.variant).toSet

    test.canRun(fakeRequest) ==> (allocatedVariants == test.variants.toSet) ||
      !test.canRun(fakeRequest) ==> (allocatedVariants == Set.empty)
  }
}
