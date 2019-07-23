package com.eztier.datasource.mongodb.hl7.runners

import akka.stream.scaladsl.Source
import com.eztier.datasource.mongodb.hl7.implicits._
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions

object CommandRunner {
  def search[A](from: Long, to: Long)(implicit searchable: Searchable[A]): Option[Source[A, akka.NotUsed]] = {
    searchable.search(from, to)
  }

  def searchWithProjections[A](from: Long, to: Long, projection: Option[conversions.Bson] = None)(implicit searchable: Searchable[A]): Option[Source[Document, akka.NotUsed]] = {
    searchable.searchWithProjections(from, to, projection)
  }

  def insert[A](a: A)(implicit insertable: Insertable[A]) = {
    insertable.insert(a)
  }

  def update[A](a: A)(implicit updatable: Updatable[A]) = {
    updatable.update(a)
  }

  def findOne[A](filter: Option[conversions.Bson] = None)(implicit searchable: Searchable[A]) = {
    searchable.findOne(filter)
  }
}
