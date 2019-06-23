package com.eztier.datasource.mongodb.hl7.models

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

/*
Note: DateTimeofBirth is used.  Not DateTimeOfBirth.
*/

case class ResearchPatient (
  _id: String = "",
  mrn: String = "",
  PatientIdentifierList: String = "",
  PatientName: String = "",
  DateTimeofBirth: String = "",
  AdministrativeSex: String = "",
  Race: String = "",
  PatientAddress: String = "",
  PhoneNumberHome: String = "",
  EthnicGroup: String = "",
  dateCreated: Long = 0,
  dateTimezoneOffset: Int = 0,
  dateLocal: String = ""
){
  import ResearchPatient._

  def toRawHl7: String = createMinimalHl7Message(this)
}

object ResearchPatient {
  def createMinimalHl7Message(r: ResearchPatient) = {

    val rg = 0 to 39

    val pid = rg./: ("PID|") {
      (a, i) =>
        i match {
          case 0 => a + "1|"
          case 2 => a + r.PatientIdentifierList + "|"
          case 4 => a + r.PatientName + "|"
          case 6 => a + r.DateTimeofBirth.replace("-", "") + "|"
          case 7 => a + r.AdministrativeSex + "|"
          case 9 => a + r.Race + "|"
          case 10 => a + r.PatientAddress + "|"
          case 12 => a + r.PhoneNumberHome + "|"
          case 21 => a + r.EthnicGroup + "|"
          case _ => a + "|"
        }
    }

    val dateCreated = LocalDateTime.ofInstant(Instant.ofEpochMilli(r.dateCreated), ZoneId.systemDefault())
    val dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    val formatedDate = dateCreated.format(dtf)

    val ctrl = r.mrn + formatedDate

    val msh = s"MSH|^~\\&|SENDING_APPLICATION|SENDING_FACILITY|RECEIVING_APPLICATION|RECEIVING_FACILITY|$formatedDate||ADT^A08|$ctrl|P|2.3||||\r";

    msh + pid + "\r"
  }
}
