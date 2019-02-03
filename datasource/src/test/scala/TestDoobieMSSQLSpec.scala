package com.eztier.test

import java.util.Date
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfter, Failed, FunSpec, Matchers}

import cats.effect.IO
import doobie._
import doobie.implicits._

import com.eztier.datasource.mssql.dwh.models.ExecutionLog
import com.eztier.datasource.mssql.dwh.implcits.Transactors._

// sbt "project datasource" testOnly *TestDoobieMSSQLSpec
class TestDoobieMSSQLSpec extends FunSpec with Matchers {

  describe("Doobie MSSQL Suite") {
    it("Construct valid SQL statement") {
      val schema = "buzz"
      val toStore = "foobar2"
      val fromDateTime: DateTime = new DateTime("2019-01-31T12:43:03.141Z")
      val toDateTime: DateTime = new DateTime("2019-02-03T18:58:50.141Z")
      
      val stmt = fr"select start_time StartTime from " ++ 
        Fragment(schema, None) ++ fr".wsi_execution_hist where to_store = " ++ 
        Fragment(s"'$toStore'", None) ++ fr" and start_time >= " ++
        Fragment(s"'${fromDateTime.toLocalDateTime.toString()}'", None) ++ fr" and start_time <= " ++
        Fragment(s"'${toDateTime.toLocalDateTime.toString()}'", None)

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
