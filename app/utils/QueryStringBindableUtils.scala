package utils

import java.net.URLEncoder

import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.QueryStringBindable

import scala.reflect.ClassTag
import scala.util.Try

object QueryStringBindableUtils {

  /**
    * Utility function for building query string bindables.
    * Useful when there is one value under the given query string key.
    */
  def queryStringBindableInstance[A : ClassTag](
      decoder: String => Option[A],
      encoder: A => String
  ): QueryStringBindable[A] = new QueryStringBindable[A] {
    import cats.syntax.either._

    // No need to URL decode since netty does this already (as written in the Play documentation)
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, A]] =
      params.get(key).map { values =>
        for {
          encodedValue <- Either.fromOption(
            values.headOption,
            s"no value for key $key in query string"
          )
          decodedValue <- Either.fromOption(
            decoder(encodedValue),
            s"value of key $key in query string can't be decoded to an instance of ${reflect.classTag[A].runtimeClass}"
          )
        } yield decodedValue
      }

    override def unbind(key: String, value: A): String =
      Try(URLEncoder.encode(encoder(value), "utf-8")).toOption
        .map(key + "=" + _)
        .getOrElse("")
  }

  /**
    * Use when the value is encoded as Json.
    */
  def queryStringBindableInstanceFromFormat[A : ClassTag : Reads : Writes]: QueryStringBindable[A] =
    queryStringBindableInstance(Json.parse(_).validate[A].asOpt, Json.toJson(_).toString)

  object Syntax {

    implicit class QueryStringBindableOps[A](a: A)(implicit B: QueryStringBindable[A]) {
      def encodeQueryString(key: String): String = B.unbind(key, a)
    }
  }
}
