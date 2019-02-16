package com.eztier.datasource.mongodb.hl7.runners

import akka.stream.scaladsl.Source
import org.mongodb.scala.MongoCollection
import com.eztier.datasource.mongodb.hl7.implicits._

object CommandRunner {
  def search[A](from: Long, to: Long)(implicit searchable: Searchable[A]): Option[Source[A, akka.NotUsed]] = {
    searchable.search(from, to)
  }
}
