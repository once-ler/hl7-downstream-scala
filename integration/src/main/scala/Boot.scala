package com.eztier.integration.hl7

import com.eztier.integration.hl7.Hl7MongoToCassandra._
import com.eztier.integration.hl7.CaPatientToCassandra._
import com.eztier.integration.hl7.CsvToCassandra._

object Boot extends App {
  // val r = runMongoToCassandra
  // println(r)

  // val r2 = runCaPatientToCassandra
  // println(r2)

  val r3 = runCsvToCassandra()
  println(r3)

  System.exit(0)

}