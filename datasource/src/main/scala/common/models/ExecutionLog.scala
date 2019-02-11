package com.eztier.datasource.common.models

import doobie.util.Meta
import java.time.LocalDateTime

// circe unmarshalling DateTime support
import io.circe.generic.semiauto._
import io.circe.java8.time._

case class ExecutionLog(
  StartTime: Option[LocalDateTime],
  FromStore: Option[String],
  ToStore: Option[String],
  StudyId: Option[String],
  WSI: Option[String],
  Caller: Option[String],
  Request: Option[String],
  Response: Option[String],
  Error: Option[Boolean]
)

case class ExecutionLogMini(
  StartTime: LocalDateTime,
  FromStore: String,
  ToStore: String,
  StudyId: String,
  WSI: String,
  Caller: String,
  Response: String
)

object ExecutionLogImplicits {
  /* Reference joda/java8 <-> java sql timestamp
    * JodaTime
    To convert JodaTime's org.joda.time.LocalDate to java.sql.Timestamp, just do:
      Timestamp timestamp = new Timestamp(localDate.toDateTimeAtStartOfDay().getMillis());

    To convert JodaTime's org.joda.time.LocalDateTime to java.sql.Timestamp, just do:
      Timestamp timestamp = new Timestamp(localDateTime.toDateTime().getMillis());

    * JavaTime
    To convert Java8's java.time.LocalDate to java.sql.Timestamp, just do:
      Timestamp timestamp = Timestamp.valueOf(localDate.atStartOfDay());

    To convert Java8's java.time.LocalDateTime to java.sql.Timestamp, just do:
      Timestamp timestamp = Timestamp.valueOf(localDateTime);
  */

  // doobie meta for LocalDateTime
  /**
    * imap:
    * def imap[B](f: A => B)(g: B => A): Meta[B]
    ***/
  implicit val LocalDateTimeMeta: Meta[LocalDateTime] =
    Meta[java.sql.Timestamp].imap(_.toLocalDateTime)(java.sql.Timestamp.valueOf)


  // circe encoder/deconder for LocalDateTime
  implicit val AEncoder = deriveEncoder[ExecutionLogMini]
  implicit val ADecoder = deriveDecoder[ExecutionLogMini]

  implicit val BEncoder = deriveEncoder[ExecutionLog]
  implicit val BDecoder = deriveDecoder[ExecutionLog]
}
