package com.eztier.datasource.mongodb.hl7.implicits

import org.mongodb.scala.{Document, MongoClient, MongoCollection}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}

import com.eztier.common.Configuration._
import com.eztier.datasource.mongodb.hl7.models.Hl7Message

object Transactors {
  val url = conf.getString(s"$env.alpakka.mongodb.hl7.url")
  val database = conf.getString(s"$env.alpakka.mongodb.hl7.database")
  val collection = conf.getString(s"$env.alpakka.mongodb.hl7.collection")

  private val client = MongoClient(url)
  private val codecRegistry = fromRegistries(fromProviders(classOf[Hl7Message]), DEFAULT_CODEC_REGISTRY)

  private val db = client.getDatabase(database).withCodecRegistry(codecRegistry)
  // private val hl7Coll = db.getCollection("hl7")

  implicit val xa: MongoCollection[Hl7Message] = db.getCollection(collection)

}
