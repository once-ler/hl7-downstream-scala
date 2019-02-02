package com.eztier.datasource.postgres.eventstore.implcits

import java.util.Date

// import akka.actor.ActorSystem
// import akka.stream.ActorMaterializer
// import akka.event.LoggingAdapter
import doobie._
import doobie.implicits._

// import doobie.postgres._
import doobie.postgres.implicits._

import cats.implicits._ // Required for Foldable[F] for VersionControlRow type
import cats.effect.IO
// import scala.concurrent.Future
import scala.reflect.runtime.universe._

import com.eztier.cassandra.CaCommon._
import com.eztier.hl7mock.types.CaPatient
import com.eztier.datasource.postgres.eventstore.models.CaPatient._
import com.eztier.datasource.postgres.eventstore.models._
import com.eztier.datasource.common.models.{Patient, Model}

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

trait Updatable[A] {
  def update(list: A, schema: String = "hl7")(implicit xa: Transactor[IO], typeTag: TypeTag[A]): IO[Int]
  // def update(list: List[A], schema: String = "hl7")(implicit xa: Transactor[IO], typeTag: TypeTag[A]): IO[List[A]]
}

/*
  @params
    useSchemaTablespace: Boolean
      true: assumption that a tablespace with schema as its name has already been set up
      false: will use the default "pg_default" tablespace
*/
trait Creatable[A] {
  def create(primaryKeys: List[String], schema: String = "hl7", useSchemaTablespace: Boolean = false)(implicit xa: Transactor[IO], typeTag: TypeTag[A]): IO[Int]
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

object Updatable {
  implicit object VersionControlUpdate extends Updatable[VersionControl] {
    override def update(a: VersionControl, schema: String = "hl7")
    (implicit xa: Transactor[IO], typeTag: TypeTag[VersionControl]): IO[Int] = {

      type VersionControlRow = (String, String, Date)

      val row = (a.model, a.subscriber, a.startTime)
      val tname = camelToUnderscores(typeTag.tpe.typeSymbol.name.toString)

      val stmt = s"""insert into $schema.$tname (model, subscriber, start_time) values (?, ?, ?)
      on conflict(model, subscriber) do update set start_time = EXCLUDED.start_time"""

      Update[VersionControlRow](stmt)
        .toUpdate0(row)
        .run
        .transact(xa)

    }
  }
/**
  implicit object CaPatientControlUpdate extends Updatable[CaPatientControl] {
    override def update(list: List[CaPatientControl], schema: String = "hl7")
    (implicit xa: Transactor[IO], typeTag: TypeTag[CaPatientControl]): IO[List[CaPatientControl]] = {

      type VersionControlRow = (String, String, Date)

      val tname = camelToUnderscores(typeTag.tpe.typeSymbol.name.toString)

      val rows = list.map(a => (tname, a.subscriber, a.startTime))

      val stmt = s"""insert into $schema.model_last_execution (model, subscriber, start_time) values (?, ?, ?)
      on conflict(model, subscriber) do update set start_time = EXCLUDED.start_time"""

      Update[VersionControlRow](stmt)
        .updateManyWithGeneratedKeys[CaPatientControl]("model", "subscriber", "start_time")(rows)
        .compile.toList.transact(xa)

    }
  }
**/
}

object Creatable {
  implicit object VersionControlCreate extends Creatable[VersionControl] {
    override def create(primaryKeys: List[String], schema: String = "hl7", useSchemaTablespace: Boolean = false)
    (implicit xa: Transactor[IO], typeTag: TypeTag[VersionControl]): IO[Int] = {
      val tname = camelToUnderscores(typeTag.tpe.typeSymbol.name.toString)

      val tblspc = if (useSchemaTablespace) schema else "pg_default"
      val pk = primaryKeys.mkString(",")
      val stmt = fr"create table if not exists " ++ Fragment(s"$schema.$tname", None) ++ fr""" (
        |model varchar(120),
        |subscriber varchar(120),
        |start_time timestamp with time zone,
        |primary key (""".stripMargin ++ Fragment(pk, None) ++ fr") ) tablespace " ++ Fragment(tblspc, None)

      stmt.update.run.transact(xa)
    }
  }
}
