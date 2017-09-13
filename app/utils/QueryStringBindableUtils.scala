package utils

import play.api.mvc.QueryStringBindable

object QueryStringBindableUtils {

  trait QueryParamsFormat[A] extends QueryStringBindable[A] {
    import cats.syntax.either._

    def key: String

    def encodeQueryString(a: A): String = unbind(key, a)

    def decodeQueryString(params: Map[String, Seq[String]]): Either[String, A] =
      bind(key, params).getOrElse(Either.left(s"key $key not found in query string"))
  }

  object QueryParamsFormat {
    def apply[A](implicit F: QueryParamsFormat[A]): QueryParamsFormat[A] = F
  }

  object Syntax {

    implicit class QueryParamsFormatOps[A](a: A)(implicit B: QueryParamsFormat[A]) {
      def encodeQueryString: String = B.encodeQueryString(a)
    }
  }
}



