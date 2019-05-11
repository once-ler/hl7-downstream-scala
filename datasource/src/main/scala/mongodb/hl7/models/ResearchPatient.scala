package com.eztier.datasource.mongodb.hl7.models

case class ResearchPatient (
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