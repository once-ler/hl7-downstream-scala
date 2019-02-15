package com.eztier.datasource.mongodb.hl7.models

case class Hl7Message(
  mrn: String,
  messageType: String,
  dateCreated: Long,
  dateTimezoneOffset: Int,
  dateLocal: String,
  raw: Seq[String]
)
