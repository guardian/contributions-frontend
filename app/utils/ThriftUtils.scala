package utils

import com.twitter.scrooge.ThriftEnum
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.AcquisitionSource
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.libs.json.{JsError, JsSuccess, Reads}
import simulacrum.typeclass

import scala.reflect.ClassTag

case class ThriftDecodeError private (message: String)

object ThriftDecodeError {

  def apply[A : ClassTag](value: String): ThriftDecodeError = {
    val clazz = reflect.classTag[A].runtimeClass
    new ThriftDecodeError(s"""unable to convert string "$value" to an instance of $clazz""")
  }
}

@typeclass trait ThriftEnumFormatter[A] {

  def decode(enum: String): Either[ThriftDecodeError, A]

  def encode(a: A): String
}

object ThriftEnumFormatter {

  def fromScroogeValueOf[A <: ThriftEnum : ClassTag](valueOf: String => Option[A]) = new ThriftEnumFormatter[A] {
    import cats.syntax.either._

    override def decode(enum: String): Either[ThriftDecodeError, A] =
      Either.fromOption(valueOf(enum.filter(_ != '_')), ThriftDecodeError[A](enum))

    override def encode(a: A): String = a.name
  }
}

object ThriftUtils {

  object Implicits {

    implicit val componentTypeThriftEnumFormatter: ThriftEnumFormatter[ComponentType] =
      ThriftEnumFormatter.fromScroogeValueOf(ComponentType.valueOf)

    implicit val acquisitionSourceThriftEnumFormatter: ThriftEnumFormatter[AcquisitionSource] =
      ThriftEnumFormatter.fromScroogeValueOf(AcquisitionSource.valueOf)

    implicit def thriftEnumReads[A](implicit F: ThriftEnumFormatter[A]): Reads[A] = Reads { json =>
      json.validate[String].flatMap { raw =>
        F.decode(raw).fold(err => JsError(err.message), enum => JsSuccess(enum))
      }
    }

    implicit def thriftEnumFormatter[A](implicit F: ThriftEnumFormatter[A]): Formatter[A] = new Formatter[A] {
      import cats.syntax.either._

      private def decodeFormValueOfKey(key: String)(enum: String): Either[FormError, A] =
        F.decode(enum).leftMap(err => FormError(key, err.message))

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        Either.fromOption(data.get(key), FormError(key, s"unable to find the key $key in the form"))
        .flatMap(decodeFormValueOfKey(key))
        .leftMap(err => Seq(err))

      override def unbind(key: String, value: A): Map[String, String] = Map(key -> F.encode(value))
    }
  }
}
