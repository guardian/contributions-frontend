package views.support

import actions.CommonActions.MetaDataRequest
import com.gu.i18n.CountryGroup

case class PageInfo(
  title: String = "Support the Guardian",
  url: String = "/",
  description: Option[String] = None,
  image: Option[String] = None,
  customSignInUrl: Option[String] = None,
  stripePublicKeys: Map[CountryGroup, String] = Map.empty,
  kruxId: String = "JglooLwn"
)
