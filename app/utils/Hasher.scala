package utils

import java.nio.charset.StandardCharsets

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing

trait Hasher {

  // Should apply a cryptographic hash function to the data.
  // https://en.wikipedia.org/wiki/Cryptographic_hash_function
  def hash(data: String): String
}

object SHA256Hasher extends Hasher {

  private val sha256: HashFunction = Hashing.sha256()

  // Exposed for unit testing
  def hashWithoutSalt(data: String): String =
    sha256.hashString(data, StandardCharsets.UTF_8).toString

  // Randomly generated global salt to prevent against dictionary attacks
  // https://en.wikipedia.org/wiki/Dictionary_attack
  // DO NOT EDIT!
  val salt = "9Bc671CC30Ee4B24"

  override def hash(data: String): String = hashWithoutSalt(salt + data)
}
