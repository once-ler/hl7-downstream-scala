package com.eztier.integration.hl7

import com.eztier.integration.hl7.Hl7MongoToCassandra._

object Boot extends App {
  val r = streamMongoToCassandra
  // Should be 0 if there is nothing to process.
  println(r)
}