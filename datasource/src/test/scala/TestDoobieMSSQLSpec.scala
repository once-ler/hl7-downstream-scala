package com.eztier.test

import java.util.Date

import com.eztier.datasource.mssql.dwh.runners.CommandRunner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.{ISO_LOCAL_DATE, ISO_LOCAL_DATE_TIME, ISO_LOCAL_TIME, ISO_OFFSET_DATE_TIME, ISO_OFFSET_TIME, ISO_ZONED_DATE_TIME}

import org.scalatest.{BeforeAndAfter, Failed, FunSpec, Matchers}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.Sink

import scala.concurrent.Await
import scala.concurrent.duration._
import cats.effect.IO
import doobie._
import doobie.implicits._
import com.eztier.datasource.common.models.ExecutionLog
import com.eztier.datasource.common.models.ExecutionLogImplicits._
import com.eztier.datasource.mssql.dwh.implicits.Transactors
import com.eztier.datasource.mssql.dwh.implicits.Transactors._

// sbt "project datasource" testOnly *TestDoobieMSSQLSpec
class TestDoobieMSSQLSpec extends FunSpec with Matchers {

  describe("Doobie MSSQL Suite") {
    implicit val system = ActorSystem("Sys")
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val schema = "ril"
    val toStore = "store_def"
    // val fromDateTime: DateTime = new DateTime("2019-01-31T12:43:03.141Z")
    // val toDateTime: DateTime = new DateTime("2019-02-03T18:58:50.141Z")
    val fromDateTime: LocalDateTime = LocalDateTime.parse("2019-01-31T12:43:03.141", ISO_LOCAL_DATE_TIME)
    val toDateTime: LocalDateTime = LocalDateTime.parse("2019-02-03T18:58:50.141", ISO_LOCAL_DATE_TIME)

    it("Search using command runner") {
      val resp = CommandRunner
        .search[ExecutionLog](toStore, fromDateTime, toDateTime)
        .throttle(elements = 100, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
        .runWith(Sink.seq)

      val r1 = Await.result(resp, 2000 millis)

      r1.foreach(println(_))

    }

    it("Construct valid SQL statement") {
      
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
      // val xa = implicitly[Transactor[IO]]
      val y = xa.yolo
      import y._
      
      stmt
        .query[ExecutionLog]
        .check
        .unsafeRunSync
      // Fin Testing

    }

  }

} 
