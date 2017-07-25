package controllers

import akka.util.ByteString
import play.api.data.Form
import play.api.libs.streams.Accumulator
import play.api.mvc.BodyParsers.parse
import play.api.mvc.{BodyParser, RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

object BodyParsers {

  implicit class BodyParserOps[A](val bodyParser: BodyParser[A]) extends AnyVal {
    def logFailure(f: RequestHeader => Unit)(implicit ec: ExecutionContext): BodyParser[A] = new BodyParser[A] {
      import cats.syntax.either._
      override def apply(header: RequestHeader): Accumulator[ByteString, Either[Result, A]] = {
        bodyParser(header).map(_.leftMap { result =>
          f(header)
          result
        })
      }
    }
  }

  // this applies the constraints of a form on a JSON payload
  def jsonFormBodyParser[A](form: Form[A])(implicit ec: ExecutionContext): BodyParser[A] = BodyParser { requestHeader =>
    parse.anyContent(None)(requestHeader).map { resultOrBody =>
      resultOrBody.right.flatMap { body =>
        body.asJson.map(json => form.bind(json)
          .fold(_ => Left(Results.BadRequest), a => Right(a)))
          .getOrElse(Left(Results.BadRequest))
      }
    }
  }

  /**
    * Takes a form, either as a multipart post, or as a json payload
    * @param form the form to parse (will respect format constraint, even in json)
    * @tparam A the result type
    * @return a body parser
    */
  def jsonOrMultipart[A](form: Form[A])(implicit ec: ExecutionContext): BodyParser[A] = parse.using { request =>
    request.contentType match {
      case Some("multipart/form-data") => parse.form(form)
      case Some("application/json") => jsonFormBodyParser(form)
      case _ => parse.error(Future.successful(Results.BadRequest))
    }
  }

}
