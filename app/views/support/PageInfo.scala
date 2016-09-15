package views.support

case class PageInfo(
  title: String = "Support the Guardian",
  url: String = "/",
  description: Option[String] = None,
  image: Option[String] = None,
  customSignInUrl: Option[String] = None,
  stripePublicKey: Option[String] = None
)
