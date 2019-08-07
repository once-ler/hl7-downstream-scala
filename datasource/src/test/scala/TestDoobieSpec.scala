package com.eztier.test

import java.util.Date

import org.scalatest.{BeforeAndAfter, Failed, FunSpec, Matchers}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.eztier.datasource.postgres.eventstore.models.VersionControl
import com.eztier.datasource.common.runners.{CommandRunner => CommandRunnerCommon}
import com.eztier.datasource.postgres.eventstore.implicits.Transactors
import com.eztier.datasource.postgres.eventstore.runners.CommandRunner
import com.eztier.hl7mock.types.CaPatient

// Required for implicitly converting java.sql.Timestamp -> java.time.LocalDateTime
import com.eztier.datasource.common.models._
import com.eztier.datasource.common.models.ExecutionLogImplicits._

// For yolo xa
// import org.joda.time.DateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_OFFSET_TIME,
  ISO_ZONED_DATE_TIME
}
import java.sql.Timestamp
import cats.effect.IO
import doobie._
import doobie.implicits._
import com.eztier.datasource.postgres.eventstore.implicits.Transactors._

import scala.concurrent.Await
import scala.concurrent.duration._

// testOnly *TestDoobieSpec
// to run only the tests whose name includes the substring "foo". -z foo
// Exact match -t foo i.e.  testOnly *TestDoobieSpec -- -t foo
class TestDoobieSpec extends FunSpec with Matchers {
  implicit val system = ActorSystem("Sys")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  implicit val cs = Transactors.cs

  implicit val xa = Transactor.fromDriverManager[IO](
    Transactors.driver,     // driver classname
    Transactors.url,     // connect URL (driver-specific)
    Transactors.user,                  // user
    Transactors.pass,                          // password
    ExecutionContexts.synchronous // just for testing
  )

  it("Construct valid SQL statement") {
      
    val schema = "ril"
    val toStore = "store_def"
    // val fromDateTime: DateTime = new DateTime("2019-01-31T12:43:03.141Z")
    // val toDateTime: DateTime = new DateTime()
    val fromDateTime: LocalDateTime = LocalDateTime.parse("2019-01-31T12:43:03.141", ISO_LOCAL_DATE_TIME)
    val toDateTime: LocalDateTime = LocalDateTime.now()

    val stmt = fr"""select 
      start_time StartTime, 
      from_store FromStore,
      to_store ToStore,
      study_id StudyId,
      wsi WSI,
      caller Caller,
      request Request,
      response Response,
      error Error
      from """ ++ 
      Fragment(schema, List()) ++ fr".wsi_execution_hist where to_store = " ++ 
      Fragment(s"'$toStore'", List()) ++ fr" and start_time >= " ++
      Fragment(s"'${fromDateTime.toString()}'", List()) ++ fr" and start_time <= " ++
      Fragment(s"'${toDateTime.toString()}'", List())

      // Testing
      val y = xa.yolo
      import y._
      
      stmt
        .query[ExecutionLog]
        .check
        .unsafeRunSync

      stmt
        .query[ExecutionLog]
        .quick
        .unsafeRunSync

      sql"""select 
      start_time StartTime, 
      from_store FromStore, 
      to_store ToStore, 
      study_id StudyId, 
      wsi WSI, caller Caller, 
      case strpos(response, '<') when 0 
      then case when length(response) <= 195 then response else '... ' || substring(response from 195 for 192) end || ' ...' 
      else case when length(response) <= 195 then response else '... ' || substring(response from length(response) - 194 for 195) end 
      end Response 
      from ril.wsi_execution_hist limit(20)"""
        .query[ExecutionLogMini]
        .quick
        .unsafeRunSync

      sql"""select symbol, date, price from ril.wsi_execution_error_agg
      """
      .query[ExecutionAggregationLog]
      .stream
      .take(10)
      .quick
      .unsafeRunSync

      sql"select start_time, Response from ril.wsi_execution_hist"
        .query[(Timestamp, String)]
        .stream
        .take(5)
        .quick
        .unsafeRunSync
      // Fin Testing
  }

  it("Can select multi columns into a type") {
    val q0 = sql"select symbol, date, price from ril.wsi_execution_error_agg limit 5"
      .query[ExecutionAggregationLog]

    val g = CommandRunnerCommon.adhoc(q0)
      .runWith(Sink.seq)

    val r1 = Await.result(g, 500 millis)

    r1.foreach(println(_))
  }

  it("Can select multi columns into a list of tuples") {
    val q0 = sql"""select 
      start_time StartTime, 
      from_store FromStore, 
      to_store ToStore, 
      study_id StudyId, 
      wsi WSI, caller Caller, 
      response Response 
      from ril.wsi_execution_hist limit(20)"""
      .query[(Timestamp, String, String, String, String, String, String)]

    val g = CommandRunnerCommon.adhoc(q0)
      .runWith(Sink.seq)

    val r1 = Await.result(g, 500 millis)

    r1.foreach(println(_))
  }

  it("Create VersionControl table") {
    val f = CommandRunner.create(List("model", "subscriber"), "hl7", false)
      .runWith(Sink.seq)

    val r1 = Await.result(f, 500 millis)
    println(r1)
  }

  it("Can update VersionControl table") {
    val c = VersionControl("some_model", "awesome_app", LocalDateTime.now())
    val h1 = CommandRunner.update(c, "hl7")
      .runWith(Sink.head)

    val r = Await.result(h1, 500 millis)
    r should be (1)
  }

  it("Can fetch VersionControl table") {
    val g = CommandRunner.adhoc[VersionControl](s"""select
      model, subscriber, start_time from hl7.version_control
      where model = 'some_model' and subscriber = 'awesome_app' limit 1""")
      .runWith(Sink.seq)

    val r = Await.result(g, 500 millis).headOption

    r should not be (List())
  }

/*
  // Adhocable[ExecutionLog]
  val g = CommandRunner.adhoc[ExecutionLog](s"""select 
    start_time StartTime, from_store FromStore, to_store ToStore, study_id StudyId, wsi WSI, caller Caller, response Response 
    from ril.wsi_execution_hist limit(20)""")
    .runWith(Sink.seq)

  val r1 = Await.result(g, 500 millis)

  r1.foreach(println(_))
*/

/*
  // Updatable[A]
  val c = VersionControl("ca_patient", "test-doobie", new Date())
  val h1 = CommandRunner.update(c)
    .runWith(Sink.head)

  val r2: Int = Await.result(h1, 500 millis)

  println(r2)

  // Creatable[A]
  val h = CommandRunner.create[VersionControl](List("model", "subscriber"))
    .runWith(Sink.head)

  val r3: Int = Await.result(h, 500 millis)

  println(r3)

  // Eventable[A]
  val ev = GenericEvent("test::generic::event", s"""{"id":"foo"}""")
  val h = CommandRunner.addEvent(List(ev))
    .runWith(Sink.seq)

  val r2 = Await.result(h, 500 millis)

  r2.foreach(println(_))

  // Adhocable[A]
  val g = CommandRunner.adhoc[CaPatient]("select current from hl7.patient limit(1)")
    .runWith(Sink.seq)

  val r1 = Await.result(g, 500 millis)

  r1.foreach(println(_))

  // Searchable[A]
  val f = CommandRunner.search[Patient]("c")
    .runWith(Sink.seq)

  val r = Await.result(f, 500 millis)

  r.foreach(println(_))

  println("Done")

  val f2 = CommandRunner.search[CaPatient]("a")
    .runWith(Sink.seq)

  val r2 = Await.result(f2, 500 millis)

  r2.foreach(println(_))
**/
}
