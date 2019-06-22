package com.eztier.datasource.mongodb.hl7.implicits

import org.mongodb.scala.{Document, MongoClient, MongoCollection}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import com.eztier.common.Configuration._
import com.eztier.datasource.mongodb.hl7.models.{Hl7Message, ResearchPatient}

object Transactors {
  val mongoConf = conf.getConfig(s"$env.alpakka.mongodb")
  val url = mongoConf.getString("hl7.url")
  val database = mongoConf.getString("hl7.database")

  private val client = MongoClient(url)
  private val codecRegistry = fromRegistries(fromProviders(classOf[Hl7Message], classOf[ResearchPatient]), DEFAULT_CODEC_REGISTRY)
  private val db = client.getDatabase(database).withCodecRegistry(codecRegistry)

  implicit val xaHl7Message: MongoCollection[Hl7Message] = db.getCollection("hl7")
  implicit  val xaResearchPatient: MongoCollection[ResearchPatient] = db.getCollection("patient")

}
