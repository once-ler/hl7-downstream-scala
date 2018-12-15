package com.eztier.postgres.eventstore.implcits

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
// import akka.event.LoggingAdapter
import doobie._
import doobie.implicits._

import doobie.postgres._
import doobie.postgres.implicits._

import cats.effect.IO
import scala.concurrent.Future

import com.eztier.hl7mock.types.CaPatient
import com.eztier.postgres.eventstore.models.CaPatient._
import com.eztier.postgres.eventstore.models._

trait Searchable[A] {
  def search(term: String, schema: String = "hl7")(implicit xa: Transactor[IO]): IO[List[A]]
}

trait AdHocable[A] {
  def adhoc(sqlstring: String)(implicit xa: Transactor[IO]): IO[List[A]]
}

trait Eventable[A] {
  def event(streamUuid: String, streamType: String, eventUuids: List[String], eventTypes: List[String], eventBodies: List[String], schema: String = "hl7")
    (implicit xa: Transactor[IO]): IO[List[AppendEventResult]]
}

object Searchable {
  implicit object PatientSearch extends Searchable[Patient] {
    override def search(term: String, schema: String = "hl7")(implicit xa: Transactor[IO]): IO[List[Patient]] = {
      
      // https://tpolecat.github.io/doobie/docs/17-FAQ.html#how-do-i-turn-an-arbitrary-sql-string-into-a-query0update0
      val stmt = fr"""select * from """ ++ Fragment(schema, None) ++ fr""".patient where name ~* """ ++ Fragment(s"'$term'", None) ++ fr""" limit 10"""

      // Testing
      val y = xa.yolo
      import y._
      
      stmt
        .query[Patient]
        .check
        .unsafeRunSync
      // Fin Testing
      
      stmt
        .query[Patient]
        .stream
        .compile
        .to[List]
        .transact(xa)
    }
  }

  implicit object CaPatientSearch extends Searchable[CaPatient] {
    override def search(term: String, schema: String = "hl7")(implicit xa: Transactor[IO]): IO[List[CaPatient]] = {
      
      val stmt = fr"""select current from """ ++ Fragment(schema, None) ++ fr""".patient where name ~* """ ++ Fragment(s"'$term'", None) ++ fr""" limit 10"""
      stmt
        .query[CaPatient]
        .stream
        .compile
        .to[List]
        .transact(xa)
    }
  }

}

object AdHocable {
  
  implicit object CaPatientAdhoc extends AdHocable[CaPatient] {
    override def adhoc(sqlstring: String)(implicit xa: Transactor[IO]): IO[List[CaPatient]] = {
      
      val stmt = Fragment(sqlstring, None)
      stmt
        .query[CaPatient]
        .stream
        .compile
        .to[List]
        .transact(xa)
        
    }
  }

}

object Eventable {

  implicit object GenericEventEvent extends Eventable[GenericEvent] {
    override def event(streamUuid: String, streamType: String, eventUuids: List[String], eventTypes: List[String], eventBodies: List[String], schema: String = "hl7")
      (implicit xa: Transactor[IO]): IO[List[AppendEventResult]] = {
        val uuids = eventUuids.map("''" + _ + "''").mkString(",")
        // val types = eventTypes.map("'" + _ + "'").mkString(",")
        val bodies = eventBodies.map("''" + _ + "''").mkString(",")

        val stmt = fr"select " ++ Fragment(schema, None) ++ fr".mt_append_event($streamUuid::uuid, $streamType, $eventUuids::uuid[], $eventTypes, $eventBodies::jsonb[]) result"

        stmt
          .query[AppendEventResult]
          .stream
          .compile
          .to[List]
          .transact(xa)
      }
  }
  
}
