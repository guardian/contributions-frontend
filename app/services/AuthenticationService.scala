package services

import com.gu.identity.cookie.IdentityKeys
import com.gu.identity.play.AccessCredentials.{Cookies, Token}
import com.gu.identity.play.AuthenticatedIdUser._

object AuthenticationService extends com.gu.identity.play.AuthenticationService {
  def idWebAppSigninUrl(returnUrl: String) = "about:carefulness" //Config.idWebAppSigninUrl(returnUrl)

  val identityKeys: IdentityKeys = ???

  override lazy val authenticatedIdUserProvider: Provider =
    Cookies.authProvider(identityKeys).withDisplayNameProvider(Token.authProvider(identityKeys, "membership"))

}
