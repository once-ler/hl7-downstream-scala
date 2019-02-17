package com.eztier.integration.hl7

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.datastax.driver.core.Row

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.eztier.datasource.cassandra.dwh.runners.{CommandRunner => CassandraCommandRuner}
import com.eztier.datasource.mongodb.hl7.runners.{CommandRunner => MongoCommandRunner}
import com.eztier.datasource.mongodb.hl7.models.Hl7Message
import com.eztier.hl7mock.types.{CaHl7, CaHl7Control, CaPatient, CaPatientControl}
import com.eztier.stream.CommonTask.balancer

object Hl7MongoToCassandra {
  implicit val actorSystem = ActorSystem(name = "integration-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val logger = actorSystem.log

  def getLastHl7MessageUploaded = {
    val fut = CassandraCommandRuner.search[CaHl7, CaHl7Control](s"select create_date from dwh.ca_table_date_control where id = ca_hl_7")

    val fut2: Future[Long] = fut
      .map { rs =>
        val row: Row = rs.one()
        val dt = row.getTimestamp("create_date")

        dt.getTime
      }
      .recover {
        case _ =>
          LocalDateTime
            .parse("1970-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(ZoneId.systemDefault())
            .toInstant
            .toEpochMilli
      }

    Await.result(fut2, 10 seconds)
  }

  def getMongoSource = {
    val from = getLastHl7MessageUploaded
    val to = LocalDateTime.now.atZone(ZoneId.of("America/New_York")).toInstant.toEpochMilli

    MongoCommandRunner.search[Hl7Message](from, to)
  }

  def messageToRaw = Flow[Hl7Message].map { msg =>
    msg.raw.foldLeft(""){
      (a, n) => a + n + "\r"
    }
  }

  def persistToCassandra = Flow[String].map { m =>
    val f = CassandraCommandRuner.update[CaHl7, CaHl7Control](m)

    Await.result(f, 10 seconds)
  }

  def logProgress = Flow[Seq[Int]].map { a =>
    val t = a.sum
    logger.info(s"Persisted $t messages")
    t
  }

  def streamMongoToCassandra = {
    val o = getMongoSource

    o match {
      case Some(s) =>
        val r = s
          .via(messageToRaw)
          .via(balancer(persistToCassandra,10))
          .log("Persist")
          .grouped(100000)
          .via(logProgress)
          .runWith(Sink.head)

        Await.result(r, Duration.Inf)
      case _ => 0
    }
  }

}
