package utils

import play.api.mvc.QueryStringBindable

object QueryStringBindableUtils {

  object Syntax {

    implicit class QueryStringBindableOps[A](a: A)(implicit B: QueryStringBindable[A]) {
      def encodeQueryString(key: String): String = B.unbind(key, a)
    }
  }
}
