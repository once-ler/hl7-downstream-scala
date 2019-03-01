package com.eztier.integration.hl7

import java.text.SimpleDateFormat
import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.datastax.driver.core.{Row, SimpleStatement}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.eztier.adapter.Hl7CassandraAdapter
import com.eztier.hl7mock.types._
import com.eztier.datasource.cassandra.dwh.runners.{CommandRunner => CassandraCommandRuner}
import com.eztier.datasource.cassandra.dwh.implicits.Transactors.xaCaPatient

import scala.util.{Failure, Success}

object CaPatientToCassandra {
  implicit val actorSystem = ActorSystem(name = "integration-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val logger = actorSystem.log

  private def getLastCaPatientUploaded = {
    val fut = CassandraCommandRuner.search[CaHl7, CaHl7Control](s"select create_date from dwh.ca_table_date_control where id = 'ca_patient_control' limit 1")

    val fut2: Future[LocalDateTime] = fut
      .map { rs =>
        val row: Row = rs.one()
        val dt = row.getTimestamp("create_date")

        // dt.toInstant.atZone(ZoneId.of("America/New_York")).toLocalDateTime
        dt.toInstant.atOffset(ZoneOffset.UTC).toLocalDateTime
      }
      .recover {
        case _ => LocalDateTime
          .parse("2017-06-15T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      }

    Await.result(fut2, 10 seconds)
  }

  /*
  * Unfortunately, doing it this way will cause a timeout b/c cassandra will try to fetch entire dataset.
  * */
  def streamCaPatientToCassandra = {
    val from = getLastCaPatientUploaded
    val to = from.plusHours(1).minusSeconds(1)
    val now = LocalDateTime.now()
    val adjToDt = if (to.isAfter(now)) now else to

    logger.error(s"Processing from ${from.toString} to ${adjToDt.toString}")

    // runWithRowFilter() will query from ca_table_date_control and look for id "ca_hl_7"
    val r = xaCaPatient.flow.runWithRowFilter(s"create_date > '${from.toString}' and create_date < '${adjToDt.toString}'", 10)

    logger.error(s"Persisted $r messages")

    r
  }

  def getDistinctCaControlById = {
    xaCaPatient.flow.casFlow.getSourceStream("select distinct id from dwh.ca_hl_7_control", 200)
    // val r = s.runForeach(a => println(s"${a.getString("id")} ${a.getToken("id").getValue().toString}}"))
  }

  def runCaPatientToCassandra = {
    val s = getDistinctCaControlById

    val r = xaCaPatient.flow.runWithRowSource(s, 20)

    r
  }
}
