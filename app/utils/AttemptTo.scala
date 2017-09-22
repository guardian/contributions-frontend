package utils

import cats.syntax.EitherSyntax

// TODO: have this extend all classes where attempt to semantics are used.
trait AttemptTo extends EitherSyntax {

  def attemptTo[A](description: String)(a: => A): Either[String, A] = {
    Either.catchNonFatal(a).leftMap(err => s"Unable to $description - underlying error: $err")
  }
}
