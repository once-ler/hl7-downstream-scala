package com.eztier.datasource.mongodb.hl7.implicits

import akka.NotUsed
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Source
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._

import com.eztier.datasource.mongodb.hl7.models.Hl7Message

trait Searchable[A] {
  def search(from: Long, to: Long)(implicit xa: MongoCollection[A]): Source[A, NotUsed]
}

object Searchable {
  implicit object Hl7MessageSearch extends Searchable[Hl7Message] {
    override def search(from: Long, to: Long)(implicit xa: MongoCollection[Hl7Message]) = {
      MongoSource[Hl7Message](xa.find(and(gte("dateCreated", from), lte("dateCreated", to))))
    }
  }
}
