package abtests

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalacheck.Prop.forAll

object ABTestSpec extends Properties("test") {
  val testIdGen = Gen.posNum[Int].suchThat(i => i > 0 && i <= Test.maxTestId)

  implicit val ap: Arbitrary[Percentage] = Arbitrary(Gen.posNum[Double].suchThat(_ <= 100).map(Percentage))
  implicit val av: Arbitrary[Variant] = Arbitrary(Gen.alphaStr.map(Variant))
  implicit val at: Arbitrary[Test] = Arbitrary(for {
    name <- Gen.alphaStr
    audienceSize <- arbitrary[Percentage]
    audienceOffset <- arbitrary[Percentage]
    variants <- Gen.nonEmptyContainerOf[Seq, Variant](arbitrary[Variant])
  } yield Test(name, audienceSize, audienceOffset, variants: _*))

  property("test allocation") = forAll { (test: Test) =>
    test.idRange.forall(id => test.allocate(id).nonEmpty) &&
    (1 to Test.maxTestId).diff(test.idRange).forall(id => test.allocate(id).isEmpty)
  }
}
