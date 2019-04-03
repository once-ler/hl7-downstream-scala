package com.eztier.test

import java.util.Date

import com.eztier.datasource.oracle.dwh.runners.CommandRunner
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE,
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_OFFSET_TIME,
  ISO_ZONED_DATE_TIME
}

import org.scalatest.{BeforeAndAfter, Failed, FunSpec, Matchers}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.Sink

import scala.concurrent.Await
import scala.concurrent.duration._

import cats.effect.IO
import doobie._
import doobie.implicits._

import com.eztier.datasource.oracle.dwh.models.Employee
import com.eztier.datasource.oracle.dwh.models.EmployeeImplicits._
import com.eztier.datasource.oracle.dwh.implicits.Searchable._
import com.eztier.datasource.oracle.dwh.implicits.Transactors._

// sbt "project datasource" testOnly *TestDoobieMSSQLSpec
class TestDoobieOracleSpec extends FunSpec with Matchers {

  describe("Doobie ORACLE Suite") {
    implicit val system = ActorSystem("Sys")
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val schema = "HR"
    // val fromDateTime: DateTime = new DateTime("2019-01-31T12:43:03.141Z")
    // val toDateTime: DateTime = new DateTime("2019-02-03T18:58:50.141Z")
    val fromDateTime: LocalDateTime = LocalDateTime.parse("2019-01-31T12:43:03.141", ISO_LOCAL_DATE_TIME)
    val toDateTime: LocalDateTime = LocalDateTime.parse("2019-02-03T18:58:50.141", ISO_LOCAL_DATE_TIME)

    it("Search using command runner") {
      val resp = CommandRunner
        .search[Employee](fromDateTime, toDateTime)
        .throttle(elements = 100, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
        .runWith(Sink.seq)

      val r1 = Await.result(resp, 2000 millis)

      r1.foreach(println(_))

    }

    it("Construct valid SQL statement") {

      val stmt = fr"""SELECT EMPLOYEE_ID, FIRST_NAME, LAST_NAME, JOB_ID, HIRE_DATE, SALARY
        from """ ++
        Fragment(schema, None) ++ fr".EMPLOYEES where HIRE_DATE >= " ++
        Fragment(s"to_date('${fromDateTime.toString().substring(0, 19)}', 'YYYY-MM-DD${"\"T\""}HH24:MI:SS')", None) ++ fr" and HIRE_DATE <= " ++
        Fragment(s"to_date('${toDateTime.toString().substring(0, 19)}', 'YYYY-MM-DD${"\"T\""}HH24:MI:SS')", None)

      // Testing
      // val xa = implicitly[Transactor[IO]]
      val y = xa.yolo
      import y._

      stmt
        .query[Employee]
        .check
        .unsafeRunSync
      // Fin Testing

    }

  }

}
