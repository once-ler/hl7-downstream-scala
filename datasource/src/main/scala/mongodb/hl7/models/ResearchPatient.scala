package com.eztier.datasource.mongodb.hl7.models

/*
Note: DateTimeofBirth is used.  Not DateTimeOfBirth.
*/

case class ResearchPatient (
  _id: String,
  mrn: String,
  PatientIdentifierList: String,
  PatientName: String,
  DateTimeofBirth: String,
  AdministrativeSex: String,
  Race: String,
  PatientAddress: String,
  PhoneNumberHome: String,
  EthnicGroup: String,
  dateCreated: Long,
  dateTimezoneOffset: Int,
  dateLocal: String
)