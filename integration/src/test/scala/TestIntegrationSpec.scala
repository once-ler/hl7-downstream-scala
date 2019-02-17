package com.eztier.integration.test

import java.util.Date

import akka.stream.scaladsl.Source
import ca.uhn.hl7v2.parser.PipeParser
import ca.uhn.hl7v2.util.Terser
import com.eztier.adapter.Hl7CassandraAdapter
import com.eztier.common.Configuration.{conf, env}
import com.eztier.datasource.cassandra.dwh.implicits.Transactors.keySpace

import scala.collection.JavaConverters._
import org.scalatest.{BeforeAndAfter, Failed, FunSpec, Matchers}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import com.eztier.datasource.mongodb.hl7.models.Hl7Message
import com.eztier.datasource.mongodb.hl7.runners.{CommandRunner => MongoCommandRunner}
import com.eztier.hl7mock.types.{CaHl7, CaHl7Control, CaPatient, CaPatientControl}
import org.mongodb.scala.Completed

// sbt "project integration" testOnly *TestIntegrationSpec
class TestIntegrationSpec extends FunSpec with Matchers with BeforeAndAfter {
  val fixtures = ConfigFactory.load("fixtures")
  val hl7Raw = fixtures.getString("spec-test.adt-a08")
  val hl7Arr = hl7Raw.split("\r")

  val message = Hl7Message(
    mrn = "135770",
    raw = hl7Arr,
    messageType = "ADT^A01",
    dateCreated = new Date().getTime,
    dateTimezoneOffset = 14400,
    dateLocal = "2019-02-16 20:14:57.920"
  )

  describe("Integration Suite") {
    it("Should create Hl7Message in MongoDB") {
      val f = MongoCommandRunner.insert(message)

      val r = Await.result(f, 10 seconds)

      r should not be (None)
    }

    it("Should persist HL7 message to cassandra") {
      val xaCaHl7 = Hl7CassandraAdapter[CaHl7, CaHl7Control](s"$env.cassandra", keySpace, Some(conf))
      val f = xaCaHl7.flow.runWithRawStringSource(Source.single(hl7Raw))

      val r = Await.result(f, 10 seconds)

      r should be (1)
    }

    it("Should parse HL7 message with irregular PID") {
      val pipeParser = new PipeParser()
      val message = pipeParser.parse(hl7Raw)
      val terser = new Terser(message)
      val pid = terser.get("/PID-2")

      pid should be ("135770")

    }

  }

}
