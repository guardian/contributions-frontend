package utils

import java.net.URLEncoder

import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.QueryStringBindable

import scala.reflect.ClassTag
import scala.util.Try

object QueryStringBindableUtils extends RuntimeClassUtils {

  /**
    * Utility function for building query string bindables.
    * Useful when there is one value under the given query string key.
    */
  def queryStringBindableInstance[A : ClassTag](
      decoder: String => Either[String, A],
      encoder: A => String
  ): QueryStringBindable[A] = new QueryStringBindable[A] {
    import cats.syntax.either._

    // No need to URL decode since netty does this already (as written in the Play documentation)
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, A]] =
      params.get(key).map { values =>
        for {
          encodedValue <- Either.fromOption(values.headOption, s"No value for key $key in query string.")
          decodedValue <- decoder(encodedValue)
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
  def queryStringBindableInstanceFromFormat[A : ClassTag : Reads : Writes]: QueryStringBindable[A] = {
    import cats.syntax.either._

    def decoder(data: String) = for {

      json <- Either.catchNonFatal(Json.parse(data)).leftMap { _ =>
        s"""Unable to parse \"$data\" as JSON when attempting to decode an instance of ${runtimeClass[A]}"""
      }

      instance <- json.validate[A].asEither.leftMap { _ =>
        s"Unable to decode JSON $json to an instance of ${runtimeClass[A]}"
      }

    } yield instance

    def encoder(instance: A) = Json.toJson(instance).toString

    queryStringBindableInstance(decoder, encoder)
  }

  object Syntax {

    implicit class QueryStringBindableOps[A](a: A)(implicit B: QueryStringBindable[A]) {
      def encodeQueryString(key: String): String = B.unbind(key, a)
    }
  }
}
