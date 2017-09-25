package utils

import com.gu.identity.play.AuthenticatedIdUser
import com.gu.identity.testing.usernames.TestUsernames
import play.api.mvc.RequestHeader

trait TestUserService {

  def isTestUser(request: RequestHeader): Boolean

  def isTestUser(displayName: String): Boolean
}

class DefaultTestUserService(authProvider: AuthenticatedIdUser.Provider, testUsernames: TestUsernames)
  extends TestUserService {

  override def isTestUser(request: RequestHeader): Boolean =
    request.getQueryString("_test_username")
      .orElse(request.cookies.get("_test_username").map(_.value))
      .orElse(authProvider(request).flatMap(_.displayName))
      .exists(testUsernames.isValid)

  override def isTestUser(displayName: String): Boolean =
    displayName.split(' ').headOption.exists(testUsernames.isValid)
}
