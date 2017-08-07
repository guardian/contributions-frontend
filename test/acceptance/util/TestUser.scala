package acceptance.util

import java.time.Duration._
import com.github.nscala_time.time.Imports

import scala.concurrent.duration._
import com.gu.identity.testing.usernames.TestUsernames


trait TestUserGenerator {
  private val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.testUsersSecret),
    recency = Imports.Duration.standardDays(2)
    //recency = ofDays(2)
  )

  def addTestUserCookie: Unit = Driver.addCookie("_test_username", testUsers.generate())
}
