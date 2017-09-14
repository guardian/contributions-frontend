package utils

import com.twitter.scrooge.ThriftEnum
import ophan.thrift.componentEvent.ComponentType
import ophan.thrift.event.{AbTest, AcquisitionSource}
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.libs.json._
import play.api.mvc.QueryStringBindable
import simulacrum.typeclass

import scala.collection.mutable
import scala.reflect.ClassTag

case class ThriftDecodeError private (message: String)

object ThriftDecodeError {

  def apply[A : ClassTag](value: String): ThriftDecodeError = {
    val clazz = reflect.classTag[A].runtimeClass
    new ThriftDecodeError(s"""unable to convert string "$value" to an instance of $clazz""")
  }
}

/**
  * Used to derive other type classes for Thrift enums - reads, formatters and query string bindables.
  */
@typeclass trait ThriftEnumFormatter[A] {

  def decode(enum: String): Either[ThriftDecodeError, A]

  def encode(a: A): String
}

object ThriftEnumFormatter {

  def fromScroogeValueOf[A <: ThriftEnum : ClassTag](valueOf: String => Option[A]) = new ThriftEnumFormatter[A] {
    import cats.syntax.either._

    override def decode(enum: String): Either[ThriftDecodeError, A] =
      Either.fromOption(valueOf(enum.filter(_ != '_')), ThriftDecodeError[A](enum))

    override def encode(a: A): String = {
      // The name method for a thrift enum returns a camel case string
      // e.g. the component type AcquisitionsEpic would have name "AcquisitionsEpic"
      // The corresponding encode() method would return "ACQUISITIONS_EPIC"
      val builder = new mutable.StringBuilder()
      for (c <- a.name) {
        if (c.isUpper && builder.nonEmpty) builder += '_'
        builder += c.toUpper
      }
      builder.result()
    }
  }
}

object ThriftUtils {

  object Implicits {
    import cats.syntax.either._
    import QueryStringBindableUtils._

    implicit val componentTypeThriftEnumFormatter: ThriftEnumFormatter[ComponentType] =
      ThriftEnumFormatter.fromScroogeValueOf(ComponentType.valueOf)

    implicit val acquisitionSourceThriftEnumFormatter: ThriftEnumFormatter[AcquisitionSource] =
      ThriftEnumFormatter.fromScroogeValueOf(AcquisitionSource.valueOf)

    // Json typeclasses

    implicit def thriftEnumReads[A](implicit F: ThriftEnumFormatter[A]): Reads[A] = Reads { json =>
      json.validate[String].flatMap { raw =>
        F.decode(raw).fold(err => JsError(err.message), enum => JsSuccess(enum))
      }
    }

    implicit def thriftEnumWrites[A](implicit F: ThriftEnumFormatter[A]): Writes[A] = Writes { enum =>
      JsString(F.encode(enum))
    }

    implicit val abTestReads: Reads[AbTest] = {
      import play.api.libs.functional.syntax._
      ((__ \ "name").read[String] and (__ \ "variant").read[String]) { (name, variant) =>
        AbTest(name, variant)
      }
    }

    implicit val abTestWrites: Writes[AbTest] = Writes { abTest =>
      Json.obj("name" -> abTest.name, "variant" -> abTest.variant)
    }

    // Formatter typeclasses

    private def formatterInstance[A](decoder: String => Either[String, A]) = new Formatter[A] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        (for {
          encodedValue <- Either.fromOption(data.get(key), s"unable to find the key $key in the form")
          decodedValue <- decoder(encodedValue)
        } yield decodedValue).leftMap(err => Seq(FormError(key, err)))

      override def unbind(key: String, value: A): Map[String, String] = Map.empty
    }

    implicit def thriftEnumFormatter[A](implicit F: ThriftEnumFormatter[A]): Formatter[A] =
      formatterInstance(F.decode(_).leftMap(err => err.message))

    implicit val abTestFormatter: Formatter[AbTest] = formatterInstance { json =>
      Json.parse(json).validate[AbTest].asEither.leftMap(_ => s"form value $json invalid")
    }

    // Query string bindable type classes

    implicit def thriftEnumQueryStringBindable[A : ClassTag](implicit F: ThriftEnumFormatter[A]): QueryStringBindable[A] =
      queryStringBindableInstance(F.decode(_).toOption, F.encode)

    implicit val abTestQueryStringBindable: QueryStringBindable[AbTest] =
      queryStringBindableInstanceFromFormat[AbTest]
  }
}
