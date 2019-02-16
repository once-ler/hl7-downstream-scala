package com.eztier.datasource.mongodb.hl7.implicits

import akka.NotUsed
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Source
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._
import com.eztier.datasource.mongodb.hl7.models.Hl7Message
import com.eztier.datasource.mongodb.hl7.implicits.Transactors.xaHl7Message

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait Searchable[A] {
  def search(from: Long, to: Long): Option[Source[A, NotUsed]]
}

object Searchable {
  implicit object Hl7MessageSearch extends Searchable[Hl7Message] {
    override def search(from: Long, to: Long) = {
      val s = xaHl7Message.find(and(gte("dateCreated", from), lte("dateCreated", to)))
      val b = s.first().headOption()

      val r: Option[Hl7Message] = Await.result(b, 60 seconds)

      r match {
        case Some(a) => Some(MongoSource[Hl7Message](s))
        case _ => None
      }
    }
  }
}
