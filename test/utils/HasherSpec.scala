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

  property("The application hasher should hash a browser id to the same value each time") =
    forAll(browserIds) { (browserId: String) =>
      // 10 chosen arbitrarily
      (0 to 10).map(_ => SHA256Hasher.hash(browserId)).toSet.size == 1
    }
}
