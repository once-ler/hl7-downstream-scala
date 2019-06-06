package com.eztier.integration.hl7

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.eztier.datasource.common.traits.WithCsvSuppport
import com.eztier.integration.hl7.Hl7MongoToCassandra.{batch, caToInsert, rawToCa}
import com.eztier.stream.CommonTask.balancer

import scala.concurrent.Future

object CsvToCassandra extends WithCsvSuppport {
  implicit val actorSystem = ActorSystem(name = "integration-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val logger = actorSystem.log

  def runCsvToCassandra(filePathAndName: String) = {
    importAsText(filePathAndName)
      .via(rawToCa)
      .filter(_ != None)
      .filter(_.get.Id != null)
      .via(caToInsert)
      .via(balancer(batch,100))


  }

}
