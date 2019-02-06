package com.eztier.datasource.postgres.eventstore.models

import java.util.Date
import java.sql.Timestamp
import java.time.LocalDateTime

// circe unmarshalling DateTime support
// import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.circe.java8.time._

case class ExecutionLog(
  StartTime: Timestamp,
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
  StartTime: Timestamp,
  FromStore: String,
  ToStore: String,
  StudyId: String,
  WSI: String,
  Caller: String,
  Response: String
)

object ExecutionLogMini {
  // TODO: Require encoder for outgoing http
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  implicit val dateEncoder = Encoder.encodeString.contramap[LocalDateTime](_.format(formatter))
  implicit val dateDecoder = Decoder.decodeString.emap[LocalDateTime](str => {
    Either.catchNonFatal(LocalDateTime.parse(str, formatter)).leftMap(_.getMessage)
  })
  
  implicit val AEncoder = deriveEncoder[ExecutionLogMini]
  implicit val ADecoder = deriveDecoder[ExecutionLogMini]
}
