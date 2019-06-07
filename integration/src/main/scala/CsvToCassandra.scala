package com.eztier.integration.hl7

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape}

import com.eztier.datasource.common.traits.WithCsvSuppport
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.eztier.datasource.cassandra.dwh.implicits.Transactors._

object CsvToCassandra extends WithCsvSuppport {
  implicit val actorSystem = ActorSystem(name = "integration-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val logger = actorSystem.log

  def runCsvToCassandra(filePathAndName: String = "patient3.json") = {
    val s = importAsText(filePathAndName)

    val f = xaCaHl7.flow.runWithRawStringSource(s, 20)
    val f2 = xaCaPatient.flow.runWithRawStringSource(s, 20)

    val result = for {
      r <- f
      r2 <- f2
    } yield (r, r2)

    Await.result(result, Duration.Inf)
  }

}
