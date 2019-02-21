package com.eztier.integration.hl7

import com.eztier.integration.hl7.Hl7MongoToCassandra._
import com.eztier.integration.hl7.CaPatientToCassandra._

object Boot extends App {
  // val r = streamMongoToCassandra
  val r = runMongoToCassandra
  // Should be 0 if there is nothing to process.

  // val r2 = streamCaPatientToCassandra
  // println(r2._2)
}