package com.eztier.integration

import java.time.{LocalDateTime, ZoneId}

import akka.stream.scaladsl.{Flow, Sink}
import com.datastax.driver.core.Row

import scala.concurrent.Await
import scala.concurrent.duration._
import com.eztier.datasource.cassandra.dwh.runners.{CommandRunner => CassandraCommandRuner}
import com.eztier.datasource.mongodb.hl7.runners.{CommandRunner => MongoCommandRunner}
import com.eztier.datasource.mongodb.hl7.models.Hl7Message
import com.eztier.hl7mock.types.{CaPatient, CaPatientControl}
import com.eztier.stream.CommonTask.balancer

object Hl7MongoToCassandra {

  def getLastHl7MessageUploaded = {
    val fut = CassandraCommandRuner.search[CaPatient, CaPatientControl](s"select create_date from dwh.ca_table_date_control where id = ca_hl_7")
    val rs = Await.result(fut, 10 seconds)
    val row: Row = rs.one()
    val dt = row.getTimestamp("create_date")

    dt.getTime
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
    val f = CassandraCommandRuner.update[CaPatient, CaPatientControl](m)

    Await.result(f, 10 seconds)
  }

  def streamMongoToCassandra = {
    val r = getMongoSource
      .via(messageToRaw)
      .via(balancer(persistToCassandra,10))
      .runWith(Sink.head)

    Await.result(r, Duration.Inf)
  }

}
