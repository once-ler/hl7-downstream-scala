package com.eztier.datasource.test

import java.text.SimpleDateFormat

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.v231.segment.PID
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.validation.impl.NoValidation
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

  val hapiContext = new DefaultHapiContext()
  hapiContext.setModelClassFactory(new CanonicalModelClassFactory("2.3.1"))
  hapiContext.setValidationContext(new NoValidation)
  val p = hapiContext.getPipeParser()

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
        "PID|1||035769^^^||MOUSE^MICKEY^J||19281118|M||W~B~I|123 Main St.^^Lake Buena Vista^FL^32830||(407)939-1289^^^^^^^^^theMainMouse@disney.com^|||||||||N~U|||||||||||||||||||\r"
      ).mkString("")

      hl7Msg should equal (rawStr)

      hl7Msg should include ("PID")

    }

    it("Should parse PID") {
      val hl7Msg = researchPatient.toRawHl7

      val hpiMsg = p.parse(hl7Msg)

      val pid = hpiMsg.get("PID").asInstanceOf[PID]

      val gender = pid.getSex.getValueOrEmpty

      gender should be ("M")

      val mrn = pid.getPatientIdentifierList.head.getID.toString

      mrn should be ("035769")

      val sdf = new SimpleDateFormat("yyyy-MM-dd")
      val dob = pid.getDateTimeOfBirth.getTimeOfAnEvent.getValueAsDate
      val dobStr = sdf.format(dob)

      dobStr should be ("1928-11-18")

      val ethnicity = pid.getEthnicGroup.map(_.getIdentifier.getValueOrEmpty).mkString("~")

      ethnicity should be ("N~U")

      val race = pid.getRace.map(_.getIdentifier.getValueOrEmpty).mkString("~")

      race should be ("W~B~I")
    }

  }

}
