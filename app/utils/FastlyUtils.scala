package utils

import com.gu.i18n.CountryGroup
import play.api.mvc.{Request, RequestHeader}

object FastlyUtils {

  implicit class FastlyRequest(r: RequestHeader) {

    def getFastlyCountryCode: Option[String] = r.headers.get("X-GU-GeoIP-Country-Code")

    def getFastlyCountryGroup: Option[CountryGroup] = getFastlyCountryCode.flatMap(CountryGroup.byFastlyCountryCode)

    def getFastlyCountrySubdivisionCode: Option[String] = r.headers.get("GU-ISO-3166-2")
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
