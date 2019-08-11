package com.eztier.datasource.mongodb.hl7.implicits

import com.eztier.hl7mock.types.CaPatient
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.{Macros}

object CustomCodecs {
  class JodaCodec extends Codec[DateTime] {
    override def decode(bsonReader: BsonReader, decoderContext: DecoderContext): DateTime = new DateTime(bsonReader.readDateTime())

    override def encode(bsonWriter: BsonWriter, t: DateTime, encoderContext: EncoderContext): Unit = bsonWriter.writeDateTime(t.getMillis)

    override def getEncoderClass: Class[DateTime] = classOf[DateTime]
  }

  val caPatientCodec = Macros.createCodec[CaPatient]()
}

