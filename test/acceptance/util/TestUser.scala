package acceptance.util

import java.time.Duration._
import com.github.nscala_time.time.Imports

import scala.concurrent.duration._
import com.gu.identity.testing.usernames.TestUsernames


class TestUser {
  private val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.testUsersSecret),
    recency = Imports.Duration.standardDays(2)
    //recency = ofDays(2)
  )

  private def addTestUserCookies(testUsername: String) = {
    Driver.addCookie("ANALYTICS_OFF_KEY", "true")
    Driver.addCookie("pre-signin-test-user", testUsername)
  }

  val username = testUsers.generate()
  addTestUserCookies(username)
}
