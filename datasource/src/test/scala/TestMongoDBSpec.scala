package com.eztier.datasource.test

import com.eztier.datasource.mongodb.hl7.models.ResearchPatient
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import com.eztier.datasource.mongodb.hl7.runners.CommandRunner
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters

import scala.concurrent.duration._

class TestMongoDBSpec extends FunSpec with ScalaFutures with Matchers {

  val researchPatient = ResearchPatient(
    _id = "035769",
    mrn = "035769",
    PatientIdentifierList = "035769^^^",
    PatientName = "MOUSE^MICKEY^J",
    DateTimeofBirth = "1928-11-18",
    AdministrativeSex = "M",
    Race = "W~B~I",
    PatientAddress = "123 Main St.^^Lake Buena Vista^FL^32830",
    PhoneNumberHome = "(407)939-1289^^^^^^^^^theMainMouse@disney.com^",
    EthnicGroup = "N~U",
    dateCreated = 1558806288000.0.toLong,
    dateLocal = "2019-05-25 13:44:48.788",
    dateTimezoneOffset = -14400
  )

  describe ("MongoDB spec") {

    it ("Should insert one document of Mickey") {
      val f = CommandRunner.insert[ResearchPatient](researchPatient)

      whenReady(f) {
        a => a should be (Some("The operation completed successfully"))
      }
    }

    it ("Should update one document of Mickey") {
      val f = CommandRunner.update[ResearchPatient](researchPatient)

      // Or global via config:
      // implicit override val patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(20, Millis))
      whenReady(f, timeout(2 seconds), interval(500 millis)) {
        a =>
          a match {
          case Some(b) => b.getMatchedCount should be (1)
          case _ => None
        }
      }
    }

    it ("Should find one Mickey") {
      val q: conversions.Bson = Filters.eq("_id", "035769")

      CommandRunner.findOne[ResearchPatient](Some(q))
        .futureValue should equal (Some(researchPatient))
    }

    it ("Can convert Mickey to raw HL7 string") {
      val hl7Msg = researchPatient.toRawHl7

      val rawStr = List(
        "MSH|^~\\&|SENDING_APPLICATION|SENDING_FACILITY|RECEIVING_APPLICATION|RECEIVING_FACILITY|20190525134448||ADT^A08|03576920190525134448|P|2.3||||\r",
        "PID|1||035769^^^||MOUSE^MICKEY^J||1928-11-18|M||W~B~I|123 Main St.^^Lake Buena Vista^FL^32830||(407)939-1289^^^^^^^^^theMainMouse@disney.com^|||||||||N~U|||||||||||||||||||\r"
      ).mkString("")

      hl7Msg should equal (rawStr)

      hl7Msg should include ("PID")

    }

  }

}
