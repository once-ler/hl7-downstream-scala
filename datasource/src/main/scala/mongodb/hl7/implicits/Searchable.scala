package com.eztier.datasource.mongodb.hl7.implicits

import akka.NotUsed
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Source
import org.mongodb.scala.{Completed, MongoCollection, model}
import org.mongodb.scala.model.Filters._
import com.eztier.datasource.mongodb.hl7.models.{CaPatientMongo, Hl7Message, ResearchPatient}
import com.eztier.datasource.mongodb.hl7.implicits.Transactors.{xaCaPatientMongo, xaHl7Message, xaResearchPatient}
import com.eztier.datasource.common.implicits.ExecutionContext._
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait Searchable[A] {
  def search(from: Long, to: Long): Option[Source[A, NotUsed]]
  def searchWithProjections(from: Long, to: Long, projection: Option[conversions.Bson] = None): Option[Source[Document, NotUsed]]
  def findOne(filter: Option[conversions.Bson] = None): Future[Option[A]]
}

trait Insertable[A] {
  def insert(a: A): Future[Option[Completed]]
}

trait Updatable[A] {
  def update(a: A): Future[Option[UpdateResult]]
}

object Searchable {
  implicit object Hl7MessageSearch extends Searchable[Hl7Message] {
    override def findOne(filter: Option[conversions.Bson] = None): Future[Option[Hl7Message]] = {
      val f0 = if (filter == None)
        xaHl7Message.find()
      else
        xaHl7Message.find(filter.get)

      val f1 = f0.limit(1).first().headOption()
      f1.recover { case _ => None }

      f1
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
    override def searchWithProjections(from: Long, to: Long, projection: Option[conversions.Bson]): Option[Source[Document, NotUsed]] = {
      val s = xaHl7Message.find[Document](and(gte("dateCreated", from), lte("dateCreated", to)))
      s.recover { case _ => None }

      Some(MongoSource(s))
    }
  }

  implicit object ResearchPatientSearch extends Searchable[ResearchPatient] {
    override def search(from: Long, to: Long): Option[Source[ResearchPatient, NotUsed]] = {
      val q: conversions.Bson = Filters.and(Filters.gt("dateCreated", from), Filters.lte("dateCreated", to))

      val f = xaResearchPatient.find(q)

      f.recover { case _ => None }

      Some(MongoSource(f))
    }

    // collection.find().projection(fields(include("mrn", "dateCreated"), excludeId()))
    override def searchWithProjections(from: Long, to: Long, projection: Option[conversions.Bson] = None): Option[Source[Document, NotUsed]] = {
      val q: conversions.Bson = Filters.and(Filters.gt("dateCreated", from), Filters.lte("dateCreated", to))

      val f = xaResearchPatient.find[Document](q)

      val f1 = if (projection != None) f.projection(projection.get) else f

      f1.recover { case _ => None }

      Some(MongoSource(f1))
    }

    override def findOne(filter: Option[conversions.Bson] = None): Future[Option[ResearchPatient]] = {
      val f0 = if (filter == None)
        xaResearchPatient.find()
      else
        xaResearchPatient.find(filter.get)

      val f1 = f0.limit(1).first().headOption()
      f1.recover { case _ => None }

      f1
    }
  }

  implicit object CaPatientMongoSearch extends Searchable[CaPatientMongo] {
    override def search(from: Long, to: Long): Option[Source[CaPatientMongo, NotUsed]] = {
      val q: conversions.Bson = Filters.and(Filters.gt("dateCreated", from), Filters.lte("dateCreated", to))

      val f = xaCaPatientMongo.find(q)

      f.recover { case _ => None }

      Some(MongoSource(f))
    }

    // collection.find().projection(fields(include("mrn", "dateCreated"), excludeId()))
    override def searchWithProjections(from: Long, to: Long, projection: Option[conversions.Bson] = None): Option[Source[Document, NotUsed]] = {
      val q: conversions.Bson = Filters.and(Filters.gt("dateCreated", from), Filters.lte("dateCreated", to))

      val f = xaCaPatientMongo.find[Document](q)

      val f1 = if (projection != None) f.projection(projection.get) else f

      f1.recover { case _ => None }

      Some(MongoSource(f1))
    }

    override def findOne(filter: Option[conversions.Bson] = None): Future[Option[CaPatientMongo]] = {
      val f0 = if (filter == None)
        xaCaPatientMongo.find()
      else
        xaCaPatientMongo.find(filter.get)

      val f1 = f0.limit(1).first().headOption()
      f1.recover { case _ => None }

      f1
    }
  }
}

object Insertable {
  implicit object Hl7MessageInsert extends Insertable[Hl7Message] {
    override def insert(a: Hl7Message): Future[Option[Completed]] = {
      xaHl7Message.insertOne(a).headOption
    }
  }

  implicit object ResearchPatientInsert extends Insertable[ResearchPatient] {
    override def insert(a: ResearchPatient): Future[Option[Completed]] = {
      xaResearchPatient.insertOne(a).headOption
    }
  }
}

object Updatable {
  implicit object ResearchPatientUpdate extends Updatable[ResearchPatient] {
    override def update(a: ResearchPatient): Future[Option[UpdateResult]] = {
      val options = (new model.ReplaceOptions()).upsert(true)
      val filter = Filters.eq("_id", a._id)

      xaResearchPatient.replaceOne(filter, a, options).headOption()
    }
  }
}
