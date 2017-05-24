package utils

import org.scalacheck.Gen
import org.scalacheck.Prop._
import org.scalacheck.Properties

class HasherSpec extends Properties("test") {

  private val browserIdLength = 10

  private val browserIds = Gen.listOfN(browserIdLength, Gen.alphaNumChar).map(_.mkString)

  property("The application hasher must be able to hash browser ids") =
    forAll(browserIds) { (browserId: String) =>
      browserId != SHA256Hasher.hash(browserId)
    }

  property("The application hasher must provide a different hash when a salt is added") =
    forAll(browserIds) { (browserId: String) =>
      SHA256Hasher.hash(browserId) != SHA256Hasher.hashWithoutSalt(browserId)
    }
}
