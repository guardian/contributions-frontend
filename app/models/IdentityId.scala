package models

import com.gu.identity.cookie.{IdentityCookieDecoder, ProductionKeys}
import play.api.libs.json.{Format, JsResult, JsValue, Json}
import play.api.mvc.Request

case class IdentityId(id: String) extends AnyVal {
  override def toString: String = id.toString
}

object IdentityId {

  val decoder = new IdentityCookieDecoder(new ProductionKeys)

  def fromRequest(request: Request[_]): Option[IdentityId] = {
    request.cookies.get("SC_GU_U").flatMap { cookie =>
      decoder.getUserDataForScGuU(cookie.value)
        .map(_.id)
        .map(IdentityId.apply)
    }
  }

  implicit val jf = new Format[IdentityId] {
    override def reads(json: JsValue): JsResult[IdentityId] = json.validate[String].map(IdentityId.apply)
    override def writes(o: IdentityId): JsValue = Json.toJson(o.id)
  }
}
