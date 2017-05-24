package utils

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

trait Hasher {

  // Should apply a cryptographic hash function to the data.
  // https://en.wikipedia.org/wiki/Cryptographic_hash_function
  def hash(data: String): String
}

// Facilitates interacting with the underlying message digest via strings,
// ensuring the same charset is used throughout for encoding/decoding.
private[utils] class MessageDigestWrapper private (underlying: MessageDigest, charset: Charset) {

  def digest(data: String): String = {
    val bytes = underlying.digest(data.getBytes(charset))
    new String(bytes, charset)
  }

  def setSalt(salt: String): Unit = {
    val bytes = salt.getBytes(charset)
    underlying.update(bytes)
  }
}

object MessageDigestWrapper {

  def SHA256: MessageDigestWrapper =
    new MessageDigestWrapper(MessageDigest.getInstance("SHA-256"), StandardCharsets.UTF_8)
}

class SHA256Hasher private (messageDigest: MessageDigestWrapper) extends Hasher {

  override def hash(data: String): String = messageDigest.digest(data)
}

object SHA256Hasher {

  // Randomly generated global salt to prevent against dictionary attacks
  // https://en.wikipedia.org/wiki/Dictionary_attack
  // DO NOT EDIT!
  val salt = "9Bc671CC30Ee4B24"

  val instance: SHA256Hasher = {
    val messageDigest = MessageDigestWrapper.SHA256
    messageDigest.setSalt(salt)
    new SHA256Hasher(messageDigest)
  }
}
