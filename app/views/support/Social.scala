package views.support

import java.net.URLEncoder

import configuration.{Social => SocialConfig}

sealed trait Social
case class Facebook(url: String, title: String = "") extends Social
case class LinkedIn(url: String) extends Social
case class GooglePlus(url: String) extends Social
case class Twitter(message: String) extends Social
case class Email(subject: String, message: String) extends Social

object Social {
  def encodeEmail(str: String) = URLEncoder.encode(str, "UTF-8").replaceAll("\\+", "%20")
  def encode(str: String) = URLEncoder.encode(str, "UTF-8")

  def link(s: Social) = s match {
    case Twitter(message) => s"https://twitter.com/intent/tweet?text=${Social.encode(message)}&amp;related=${SocialConfig.twitterUsername}"
    case Email(subject, body) => s"mailto:?subject=${Social.encodeEmail(subject)}&amp;body=${Social.encodeEmail(body)}"
    case Facebook(url, title) => s"https://www.facebook.com/sharer/sharer.php?u=${Social.encode(url)}&t=${Social.encode(title)}"
    case LinkedIn(url) => s"https://www.linkedin.com/shareArticle?url=${Social.encode(url)}"
    case GooglePlus(url) => s"https://plus.google.com/share?url=${Social.encode(url)}"
  }

  def metricAction(s: Social) = s match {
    case e: Email => "email"
    case t: Twitter => "twitter"
    case f: Facebook => "facebook"
    case l: LinkedIn => "linkedin"
    case g: GooglePlus => "googleplus"
  }

  def tooltip(s: Social) = s match {
    case e: Email => "Share via Email"
    case t: Twitter => "Share on Twitter"
    case f: Facebook => "Share on Facebook"
    case l: LinkedIn => "Share on LinkedIn"
    case g: GooglePlus => "Share on Google+"
  }

  def icon(s: Social) = s match {
    case e: Email => "share-email"
    case t: Twitter => "share-twitter"
    case f: Facebook => "share-facebook"
    case l: LinkedIn => "share-linkedin"
    case g: GooglePlus => "share-gplus"
  }

}
