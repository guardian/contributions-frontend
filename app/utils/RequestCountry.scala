package utils

import com.gu.i18n.CountryGroup
import play.api.mvc.Request

object RequestCountry {
  implicit class RequestWithFastlyCountry(r: Request[_]) {
    def getFastlyCountry = r.headers.get("X-GU-GeoIP-Country-Code").flatMap(CountryGroup.byFastlyCountryCode)
  }
  implicit class AuthenticatedRequestWithIdentity(r:/*Auth*/Request[_])
  {
    def getIdentityCountryGroup = CountryGroup.UK
    //FIXME:
    /*{
      implicit val identityRequest = IdentityRequest(r)
      r.touchpointBackend.identityService
        .getFullUserDetails(r.user)
        .map(
          _.privateFields
            .flatMap(_.country)
            .flatMap(CountryGroup.byCountryNameOrCode)
        )
    }*/
  }
}
