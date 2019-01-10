package com.eztier.postgres.eventstore.models

import java.util.Date
import java.text.SimpleDateFormat
import doobie._
import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._ // Required for io.circe.generic.extras.decoding.ConfiguredDecoder
import io.circe.generic.extras.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.syntax._

import com.eztier.hl7mock.types._

object CaPatient {
  import doobie.postgres.circe.jsonb.implicits._
  
  val renameKeys = (name: String) => name.charAt(0).toLower.toString + name.substring(1)

  implicit val customConfig: Configuration = Configuration.default.copy(transformMemberNames = renameKeys)
  
  implicit val dateTimeEncoder: Encoder[Date] = Encoder.instance(a => a.getTime.asJson)
  implicit val dateTimeDecoder: Decoder[Date] = Decoder.instance {
    a => 
      a.as[String].map{
        s =>
          val a = if (s.length() == 0) "1900-01-01" else s

          val fmt = a.length() match {
            case 10 => "yyyy-MM-dd"
            case 19 => "yyyy-MM-dd HH:mm:ss"
            case 23 => "yyyy-MM-dd HH:mm:ss.SSS"
          }
          val df = new SimpleDateFormat(fmt)
          df.parse(a)
      }
  }

  implicit val caPatientEncoder: Encoder[CaPatient] = deriveEncoder
  implicit val caPatientDecoder: Decoder[CaPatient] = deriveDecoder

  implicit val caPatientGet : Get[CaPatient] = pgDecoderGetT[CaPatient]
  implicit val caPatientPut : Put[CaPatient] = pgEncoderPutT[CaPatient]
}
