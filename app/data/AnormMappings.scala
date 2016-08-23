package data

import java.sql.{PreparedStatement, Timestamp}

import anorm.{ParameterMetaData, ToStatement}
import org.joda.time.DateTime

object AnormMappings {

  implicit val enumToStatement: ToStatement[DateTime] = new ToStatement[DateTime] {
    override def set(s: PreparedStatement, index: Int, v: DateTime): Unit = {
      s.setTimestamp(index, new Timestamp(v.getMillis))
    }
  }

  implicit val enumParamMeta: ParameterMetaData[DateTime] = new ParameterMetaData[DateTime] {
    override def sqlType: String = "VARCHAR"
    override def jdbcType: Int = java.sql.Types.TIMESTAMP
  }
}
