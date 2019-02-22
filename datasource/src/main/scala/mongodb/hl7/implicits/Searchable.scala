package com.eztier.datasource.mongodb.hl7.implicits

import akka.NotUsed
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Source
import org.mongodb.scala.{Completed, MongoCollection}
import org.mongodb.scala.model.Filters._
import com.eztier.datasource.mongodb.hl7.models.Hl7Message
import com.eztier.datasource.mongodb.hl7.implicits.Transactors.xaHl7Message
import com.eztier.datasource.common.implicits.ExecutionContext._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait Searchable[A] {
  def search(from: Long, to: Long): Option[Source[A, NotUsed]]
  def findOne: Option[A]
}

trait Insertable[A] {
  def insert(a: A): Future[Option[Completed]]
}

object Searchable {
  implicit object Hl7MessageSearch extends Searchable[Hl7Message] {
    override def findOne: Option[Hl7Message] = {
      val s = xaHl7Message.find().limit(1).first().headOption()
      s.recover { case _ => None }

      Await.result(s, 60 seconds)
    }

    override def search(from: Long, to: Long) = {
      val s = xaHl7Message.find(and(gte("dateCreated", from), lte("dateCreated", to)))
      val b = s.first().headOption()
      b.recover { case _ => None }

      val r: Option[Hl7Message] = Await.result(b, 60 seconds)

      r match {
        case Some(a) => Some(MongoSource[Hl7Message](s))
        case _ => None
      }
    }
  }
}

object Insertable {
  implicit object Hl7MessageUpdate extends Insertable[Hl7Message] {
    override def insert(a: Hl7Message): Future[Option[Completed]] = {
      xaHl7Message.insertOne(a).headOption
    }
  }
}
