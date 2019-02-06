package com.eztier.datasource.postgres.eventstore.models

import doobie.util.Meta
// import cats._
// import cats.implicits._

// import java.util.Date
import java.sql.Timestamp
import java.time.LocalDateTime

// circe unmarshalling DateTime support
// import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.circe.java8.time._

case class ExecutionLog(
  StartTime: LocalDateTime,
  FromStore: String,
  ToStore: String,
  StudyId: String,
  WSI: String,
  Caller: String,
  Request: String,
  Response: String,
  Error: Boolean
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

  // doobie LocalDateTime
  /**
    * imap:
    * def imap[B](f: A => B)(g: B => A): Meta[B]
  ***/
  implicit val LocalDateTimeMeta: Meta[LocalDateTime] =
    Meta[java.sql.Timestamp].imap(_.toLocalDateTime)(java.sql.Timestamp.valueOf)
    

  // circe LocalDateTime
  implicit val AEncoder = deriveEncoder[ExecutionLogMini]
  implicit val ADecoder = deriveDecoder[ExecutionLogMini]

  implicit val BEncoder = deriveEncoder[ExecutionLog]
  implicit val BDecoder = deriveDecoder[ExecutionLog]
}
