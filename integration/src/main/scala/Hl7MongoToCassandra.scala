package com.eztier.integration.hl7

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
// For akka retry
import akka.pattern.after

import com.datastax.driver.core.Row
import scala.concurrent.{Await, ExecutionContext, Future}
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
  implicit val scheduler = actorSystem.scheduler
  implicit val logger = actorSystem.log

  def getLastHl7MessageUploaded = {
    val fut = CassandraCommandRuner.search[CaHl7, CaHl7Control](s"select create_date from dwh.ca_table_date_control where id = 'ca_hl_7_control' limit 1")

    val fut2: Future[Instant] = fut
      .map { rs =>
        val row: Row = rs.one()
        val dt = row.getTimestamp("create_date")

        // dt.getTime
        dt.toInstant
      }
      .recover {
        case _ =>

          val fs = MongoCommandRunner.findOne[Hl7Message]

          fs match {
            case Some(a) => Instant.ofEpochMilli(a.dateCreated).atZone(ZoneId.systemDefault()).toInstant
            case _ =>
              LocalDateTime
                // .parse("1970-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .now.minusHours(1)
                .atZone(ZoneId.systemDefault())
                .toInstant
                // .toEpochMilli
          }
      }

    Await.result(fut2, 10 seconds)
  }

  def getMongoSource = {
    val fromDt = getLastHl7MessageUploaded.atZone(ZoneId.systemDefault()).toLocalDateTime
    val toDt = fromDt.plusHours(3)

    val from = fromDt.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
    val to = toDt.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
    // val to = LocalDateTime.now.atZone(ZoneId.of("America/New_York")).toInstant.toEpochMilli

    MongoCommandRunner.search[Hl7Message](from, to)
  }

  def messageToRaw = Flow[Hl7Message].mapAsync(parallelism = 100) { msg =>
    val m = msg.raw.foldLeft(""){
      (a, n) => a + n + "\r"
    }
    Future(m)
  }

  def retry[T](f: => Future[T], delays: Seq[FiniteDuration])(implicit ec: ExecutionContext): Future[T] = {
    f recoverWith { case _ if delays.nonEmpty => after(delays.head, scheduler)(retry(f, delays.tail)) }
  }

  def persistToCassandra = Flow[String].map { m =>
    val f = CassandraCommandRuner.update[CaHl7, CaHl7Control](m)
    f.recover{ case _ => 0 }

    val f1 = retry(f, Seq(1.seconds, 5.seconds, 10.seconds, 30.seconds, 60.seconds))

    Await.result(f1, Duration.Inf)
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
          .filter(_ != null)
          .via(balancer(persistToCassandra,10))
          .log("Persist")
          .grouped(100000)
          .via(logProgress)
          .runWith(Sink.head)

        Await.result(r, Duration.Inf)
      case _ => 0
    }
  }

  def runMongoToCassandra = {
    var r: Int = 0

    do (r = r + streamMongoToCassandra) while (r > 0)

    r
  }

}
