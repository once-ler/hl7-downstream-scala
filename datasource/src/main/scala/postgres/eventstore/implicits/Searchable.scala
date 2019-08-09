package com.eztier.datasource.postgres.eventstore.implicits

import java.util.Date

import scala.reflect.runtime.universe._
import java.time.LocalDateTime

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import cats.implicits._
import cats.effect.{IO, Resource}
import com.eztier.cassandra.CaCommon._
import com.eztier.hl7mock.types.CaPatient
import com.eztier.datasource.postgres.eventstore.models._
import com.eztier.datasource.postgres.eventstore.models.CaPatientImplicits._
import com.eztier.datasource.common.models.{Model, Patient}
import doobie.hikari.HikariTransactor

// Required for implicitly converting java.sql.Timestamp -> java.time.LocalDateTime
import com.eztier.datasource.common.models._
import com.eztier.datasource.common.models.ExecutionLogImplicits._

trait Searchable[A] {
  def search(term: String, schema: String = "hl7"): IO[List[A]]
}

trait SearchableLog[A] {
  def search(toStore: String, fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "ril"): IO[List[A]]
}

trait AdHocable[A] {
  def adhoc(sqlstring: String): IO[List[A]]
}

trait Eventable[A] {
  def event(streamUuid: String, streamType: String, eventUuids: List[String], eventTypes: List[String], eventBodies: List[String], schema: String = "hl7")
    : IO[List[AppendEventResult]]
}

trait Updatable[A] {
  def update(list: A, schema: String = "hl7")(implicit hikariTransactor: Resource[IO, HikariTransactor[IO]], typeTag: TypeTag[A]): IO[Int]
}

trait UpdateManyable[A] {
  def updateMany(list: List[A], schema: String = "hl7")(implicit hikariTransactor: Resource[IO, HikariTransactor[IO]], typeTag: TypeTag[A]): IO[Int]
}

/*
  @params
    useSchemaTablespace: Boolean
      true: assumption that a tablespace with schema as its name has already been set up
      false: will use the default "pg_default" tablespace
*/
trait Creatable[A] {
  def create(primaryKeys: List[String], schema: String = "hl7", useSchemaTablespace: Boolean = false)(implicit hikariTransactor: Resource[IO, HikariTransactor[IO]], typeTag: TypeTag[A]): IO[Int]
}

object Searchable {

  implicit object PatientSearch extends Searchable[Patient] {
    override def search(term: String, schema: String = "hl7"): IO[List[Patient]] = {

      // https://tpolecat.github.io/doobie/docs/17-FAQ.html#how-do-i-turn-an-arbitrary-sql-string-into-a-query0update0
      val stmt =
        fr"""select * from """ ++ Fragment(schema, List()) ++ fr""".patient where name ~* """ ++ Fragment(s"'$term'", List()) ++ fr""" limit 10"""

      implicit val cs = Transactors.cs
      val xa = Transactor.fromDriverManager[IO](
        Transactors.driver,     // driver classname
        Transactors.url,     // connect URL (driver-specific)
        Transactors.user,                  // user
        Transactors.pass,                          // password
        ExecutionContexts.synchronous // just for testing
      )

      // Testing
      val y = xa.yolo
      import y._

      stmt
        .query[Patient]
        .check
        .unsafeRunSync
      // Fin Testing

      Transactors.hikariTransactor.use {
        xa =>

          stmt
            .query[Patient]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }

  implicit object CaPatientSearch extends Searchable[CaPatient] {
    override def search(term: String, schema: String = "hl7"): IO[List[CaPatient]] = {

      val stmt = fr"""select current from """ ++ Fragment(schema, List()) ++ fr""".patient where name ~* """ ++ Fragment(s"'$term'", List()) ++ fr""" limit 10"""

      Transactors.hikariTransactor.use {
        xa =>
          stmt
            .query[CaPatient]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }

}

object SearchableLog {
  implicit object ExecutionLogMiniSearch extends SearchableLog[ExecutionLogMini] {
    override def search(toStore: String, fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "ril"): IO[List[ExecutionLogMini]] = {
      
      val stmt = fr"""select start_time StartTime, 
        from_store FromStore, 
        to_store ToStore, 
        study_id StudyId, 
        wsi WSI, caller Caller, 
        case strpos(response, '<') when 0 
        then case when length(response) <= 195 then response else '... ' || substring(response from 195 for 192) end || ' ...' 
        else case when length(response) <= 195 then response else '... ' || substring(response from length(response) - 194 for 195) end 
        end Response 
        from """ ++ 
        Fragment(schema, List()) ++ fr".wsi_execution_hist where error = true and to_store = " ++ 
        Fragment(s"'$toStore'", List()) ++ fr" and start_time >= " ++
        Fragment(s"'${fromDateTime.toString()}'", List()) ++ fr" and start_time <= " ++
        Fragment(s"'${toDateTime.toString()}'", List())

      Transactors.hikariTransactor.use {
        xa =>
          stmt
            .query[ExecutionLogMini]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }
}

object AdHocable {
  implicit object CaPatientAdhoc extends AdHocable[CaPatient] {
    override def adhoc(sqlstring: String): IO[List[CaPatient]] = {
      
      val stmt = Fragment(sqlstring, List())

      Transactors.hikariTransactor.use {
        xa =>
          stmt
            .query[CaPatient]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }

  implicit object ExecutionLogAdhoc extends AdHocable[ExecutionLog] {
    override def adhoc(sqlstring: String): IO[List[ExecutionLog]] = {
      
      val stmt = Fragment(sqlstring, List())

      Transactors.hikariTransactor.use {
        xa =>
          stmt
            .query[ExecutionLog]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }

  implicit object ExecutionLogMiniAdhoc extends AdHocable[ExecutionLogMini] {
    override def adhoc(sqlstring: String): IO[List[ExecutionLogMini]] = {
      
      val stmt = Fragment(sqlstring, List())

      Transactors.hikariTransactor.use {
        xa =>

          stmt
            .query[ExecutionLogMini]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }

  implicit object ExecutionAggregationLogAdhoc extends AdHocable[ExecutionAggregationLog] {
    override def adhoc(sqlstring: String): IO[List[ExecutionAggregationLog]] = {

      val stmt = Fragment(sqlstring, List())

      Transactors.hikariTransactor.use {
        xa =>
          stmt
            .query[ExecutionAggregationLog]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }

  implicit object VersionControlAdhoc extends AdHocable[VersionControl] {
    override def adhoc(sqlstring: String): IO[List[VersionControl]] = {

      val stmt = Fragment(sqlstring, List())

      //Transactors.hikariTransactor.use {
        //xa =>
          stmt
            .query[VersionControl]
            .stream
            .compile
            .to[List]
            .transact(Transactors.xa)
      //}
    }
  }

}

object Eventable {

  implicit object GenericEventEvent extends Eventable[GenericEvent] {
    override def event(streamUuid: String, streamType: String, eventUuids: List[String], eventTypes: List[String], eventBodies: List[String], schema: String = "hl7")
    : IO[List[AppendEventResult]] = {
      // val uuids = eventUuids.use("''" + _ + "''").mkString(",")
      // val types = eventTypes.use("'" + _ + "'").mkString(",")
      // val bodies = eventBodies.use("''" + _ + "''").mkString(",")

      val stmt = fr"select " ++ Fragment(schema, List()) ++ fr".mt_append_event($streamUuid::uuid, $streamType, $eventUuids::uuid[], $eventTypes, $eventBodies::jsonb[]) result"

      Transactors.hikariTransactor.use {
        xa =>
          stmt
            .query[AppendEventResult]
            .stream
            .compile
            .to[List]
            .transact(xa)
      }
    }
  }
  
}

object Updatable {
  implicit object VersionControlUpdate extends Updatable[VersionControl] {
    override def update(a: VersionControl, schema: String = "hl7")
    (implicit hikariTransactor: Resource[IO, HikariTransactor[IO]], typeTag: TypeTag[VersionControl]): IO[Int] = {

      type VersionControlRow = (String, String, LocalDateTime)

      val row = (a.model, a.subscriber, a.startTime)
      val tname = camelToUnderscores(typeTag.tpe.typeSymbol.name.toString)

      val stmt = s"""insert into $schema.$tname (model, subscriber, start_time) values (?, ?, ?)
      on conflict(model, subscriber) do update set start_time = EXCLUDED.start_time"""

      Transactors.hikariTransactor.use {
        xa =>
          Update[VersionControlRow](stmt)
            .toUpdate0(row)
            .run
            .transact(xa)
      }
    }
  }

  implicit object ExecutionLogUpdate extends Updatable[ExecutionLog] {
    override def update(a: ExecutionLog, schema: String = "ril")
    (implicit hikariTransactor: Resource[IO, HikariTransactor[IO]], typeTag: TypeTag[ExecutionLog]): IO[Int] = {
      type ExecutionLogRow = (Option[LocalDateTime], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[Boolean])

      val row = (a.StartTime, a.FromStore, a.ToStore, a.StudyId, a.WSI, a.Caller, a.Request, a.Response, a.Error)
      val tname = camelToUnderscores(typeTag.tpe.typeSymbol.name.toString)

      val stmt =
        s"""insert into $schema.$tname (
        start_time, from_store, to_store, study_id, wsi, caller, request, response, error) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        on conflict(start_time, from_store, to_store, study_id, wsi, caller)
        do update set request = EXCLUDED.request, response = EXCLUDED.response, error = EXCLUDED.error"""

      Transactors.hikariTransactor.use {
        xa =>
          Update[ExecutionLogRow](stmt)
            .toUpdate0(row)
            .run
            .transact(xa)
      }
    }
  }
}

object UpdateManyable {

  implicit object ExecutionLogBatchUpdate extends UpdateManyable[ExecutionLog] {
    override def updateMany(a: List[ExecutionLog], schema: String = "hl7")
    (implicit hikariTransactor: Resource[IO, HikariTransactor[IO]], typeTag: TypeTag[ExecutionLog]): IO[Int] = {
      // type ExecutionLogRow = (Option[LocalDateTime], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[Boolean])

      val tname = camelToUnderscores(typeTag.tpe.typeSymbol.name.toString)

      val stmt =
        s"""insert into $schema.$tname (
        start_time, from_store, to_store, study_id, wsi, caller, request, response, error) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        on conflict(start_time, from_store, to_store, study_id, wsi, caller)
        do update set request = EXCLUDED.request, response = EXCLUDED.response, error = EXCLUDED.error"""

      Transactors.hikariTransactor.use {
        xa =>
          Update[ExecutionLog](stmt)
            .updateMany(a)
            .transact(xa)
      }
    }
  }

}

object Creatable {
  implicit object VersionControlCreate extends Creatable[VersionControl] {
    override def create(primaryKeys: List[String], schema: String = "hl7", useSchemaTablespace: Boolean = false)
    (implicit hikariTransactor: Resource[IO, HikariTransactor[IO]], typeTag: TypeTag[VersionControl]): IO[Int] = {
      val tname = camelToUnderscores(typeTag.tpe.typeSymbol.name.toString)

      val tblspc = if (useSchemaTablespace) schema else "pg_default"
      val pk = primaryKeys.mkString(",")
      val stmt = fr"create table if not exists " ++ Fragment(s"$schema.$tname", List()) ++ fr""" (
        |model varchar(120),
        |subscriber varchar(120),
        |start_time timestamp with time zone,
        |primary key (""".stripMargin ++ Fragment(pk, List()) ++ fr") ) tablespace " ++ Fragment(tblspc, List())

      Transactors.hikariTransactor.use {
        xa => stmt.update.run.transact(xa)
      }

      // stmt.update.run.transact(xa)
    }
  }
}
