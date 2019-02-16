package com.eztier.integration.test

import java.util.Date

import scala.collection.JavaConverters._
import org.scalatest.{BeforeAndAfter, Failed, FunSpec, Matchers}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import com.eztier.datasource.mongodb.hl7.models.Hl7Message
import com.eztier.datasource.mongodb.hl7.runners.{CommandRunner => MongoCommandRunner}
import org.mongodb.scala.Completed

// sbt "project integration" testOnly *TestIntegrationSpec
class TestIntegrationSpec extends FunSpec with Matchers with BeforeAndAfter {
  before {

  }

  describe("Integration Suite") {
    it("Should create Hl7Message in MongoDB") {
      val fixtures = ConfigFactory.load("fixtures")
      val hl7Arr = fixtures.getString("spec-test.adt-a01").split("\r")

      val message = Hl7Message(
        mrn = "135769",
        raw = hl7Arr,
        messageType = "ADT^A01",
        dateCreated = new Date().getTime,
        dateTimezoneOffset = 14400,
        dateLocal = "2019-02-16 20:14:57.920"
      )

      val f = MongoCommandRunner.insert(message)

      val r = Await.result(f, 10 seconds)

      r should not be (None)

    }
  }

}
