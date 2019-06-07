package com.eztier.integration.hl7

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
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

  def runCsvToCassandra(filePathAndName: String = "patient4.hl7") = {
    val s = importAsText(filePathAndName)

    val f = s.grouped(100)
      .map{
        l =>
          val a = Source(l)
          val f = xaCaHl7.flow.runWithRawStringSource(a, 20)
          Await.result(f, Duration.Inf)
      }
      .runWith(Sink.head)

    val f2 = s.grouped(100)
      .map{
        l =>
          val a = Source(l)
          val f = xaCaPatient.flow.runWithRawStringSource(a, 20)
          Await.result(f, Duration.Inf)
      }
      .runWith(Sink.head)

    val result = for {
      r <- f
      r2 <- f2
    } yield (r, r2)

    Await.result(result, Duration.Inf)
  }

}
