package com.eztier.datasource.common.models

// import org.joda.time.DateTime
import java.time.LocalDateTime

// circe unmarshalling DateTime support
// import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._
import io.circe.java8.time._

case class ExecutionLogRange(toStore: String, fromDateTime: LocalDateTime, toDateTime: LocalDateTime) {
  
}

object ExecutionLogRange {
  /*
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  implicit val dateEncoder = Encoder.encodeString.contramap[LocalDateTime](_.format(formatter))
  implicit val dateDecoder = Decoder.decodeString.emap[LocalDateTime](str => {
    Either.catchNonFatal(LocalDateTime.parse(str, formatter)).leftMap(_.getMessage)
  })
  */
  implicit val AEncoder = deriveEncoder[ExecutionLogRange]
  implicit val ADecoder = deriveDecoder[ExecutionLogRange]
}
