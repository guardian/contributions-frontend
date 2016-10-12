package data

import java.sql.{PreparedStatement, Timestamp}

import anorm.{ParameterMetaData, ToStatement}
import enumeratum.EnumEntry
import org.joda.time.DateTime
import org.postgresql.util.PGobject
import play.api.libs.json.{JsValue, Json}

object AnormMappings {

  implicit val dateTimeToStatement: ToStatement[DateTime] = new ToStatement[DateTime] {
    override def set(s: PreparedStatement, index: Int, v: DateTime): Unit = {
      s.setTimestamp(index, new Timestamp(v.getMillis))
    }
  }

  implicit val dateTimeParamMeta: ParameterMetaData[DateTime] = new ParameterMetaData[DateTime] {
    override def sqlType: String = "VARCHAR"
    override def jdbcType: Int = java.sql.Types.TIMESTAMP
  }

  implicit val jsonToStatement: ToStatement[JsValue] = new ToStatement[JsValue] {
    override def set(s: PreparedStatement, index: Int, v: JsValue): Unit = {
      val jsonObj = new PGobject()
      jsonObj.setType("json")
      jsonObj.setValue(Json.stringify(v))
      s.setObject(index, jsonObj)
    }
  }

  implicit val jsonParamMeta: ParameterMetaData[JsValue] = new ParameterMetaData[JsValue] {
    override def sqlType: String = "VARCHAR"
    override def jdbcType: Int = java.sql.Types.VARCHAR
  }

  implicit def enumToStatement[A <: EnumEntry] = new ToStatement[A] {
    override def set(s: PreparedStatement, index: Int, v: A): Unit = {
      s.setString(index, v.entryName)
    }
  }

  implicit def EnumParamMeta[A <: EnumEntry] = new ParameterMetaData[A] {
    override def sqlType: String = "VARCHAR"
    override def jdbcType: Int = java.sql.Types.VARCHAR
  }
}
