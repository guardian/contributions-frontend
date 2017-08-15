package acceptance.util

import com.gu.identity.testing.usernames.TestUsernames


trait TestUserGenerator {
  private val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.testUsersSecret),
    recency = java.time.Duration.ofDays(2)
  )

  def addTestUserCookie: String = {
    val username = testUsers.generate()

    Driver.addCookie("_test_username", username)
    username
  }
}
