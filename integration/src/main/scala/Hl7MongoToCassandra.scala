package com.eztier.integration.hl7

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.Date

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import com.eztier.hl7mock.types.CaTableDateControl

import scala.concurrent.Promise
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

import scala.collection.JavaConverters._
import com.datastax.driver.core.{BatchStatement, ResultSet, ResultSetFuture}
import com.datastax.driver.core.querybuilder.{Insert}
import com.google.common.util.concurrent.{FutureCallback, Futures}
import com.eztier.cassandra.CaCommon.camelToUnderscores
import com.eztier.datasource.cassandra.dwh.implicits.Transactors._
import com.eztier.hl7mock.Hapi
import com.eztier.hl7mock.HapiToCaHl7Implicits._
import com.eztier.hl7mock.CaHl7Implicits._
import com.eztier.hl7mock.CaCommonImplicits._

object Hl7MongoToCassandra {
  implicit val actorSystem = ActorSystem(name = "integration-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val scheduler = actorSystem.scheduler
  implicit val logger = actorSystem.log

  val session = xaCaHl7.flow.provider.session

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

  val rawToCa = Flow[String].mapAsync(parallelism = 100) {
    str =>
      val m = Hapi.parseMessage(str)

      val res: Option[CaHl7] = m match {
        case Some(a) =>
          val c: CaHl7 = a
          Some(c)
        case _ => None
      }

      Future(res)
  }

  val caToInsert = Flow[Option[CaHl7]].mapAsync(parallelism = 100) {
    maybeCa =>
      val ca = maybeCa.get
      val caControl: CaHl7Control = ca

      val ins1 = ca.getInsertStatement(keySpace)
      val ins2 = caControl.getInsertStatement(keySpace)

      // val qs = ins1.getQueryString()
      Future(List[(Date, Insert)]((ca.CreateDate, ins1), (ca.CreateDate, ins2)))
  }

  // https://www.datastax.com/dev/blog/java-driver-async-queries
  implicit def resultSetFutureToScala(f: ResultSetFuture): Future[ResultSet] = {
    val p = Promise[ResultSet]()
    Futures.addCallback(f,
      new FutureCallback[ResultSet] {
        def onSuccess(r: ResultSet) = p success r
        def onFailure(t: Throwable) = p failure t
      })
    p.future
  }

  val batch = Flow[Seq[(Date, Insert)]].map {
    tup =>
      val stmts = tup.map(_._2)
      val batchStatement = new BatchStatement(BatchStatement.Type.UNLOGGED).addAll(stmts.asJava)

      val f: Future[ResultSet] = session.executeAsync(batchStatement)

      f.recover{ case _ =>  None}

      val f1 = retry(f, Seq(1.seconds, 5.seconds, 10.seconds, 30.seconds, 60.seconds))

      Await.result(f1, Duration.Inf)

      tup.map(_._1)
  }

  def retry[T](f: => Future[T], delays: Seq[FiniteDuration])(implicit ec: ExecutionContext): Future[T] = {
    f recoverWith { case _ if delays.nonEmpty => after(delays.head, scheduler)(retry(f, delays.tail)) }
  }

  val updateControl = Flow[Seq[Date]]
    .map{
      a =>
        val uts = a.max
        val c3 = CaTableDateControl(
          Id = camelToUnderscores("CaHl7Control"),
          CreateDate = uts
        )
        val ins3 = c3 getInsertStatement(keySpace)

        val f: Future[ResultSet] = session.executeAsync(ins3)

        f.recover{ case _ =>  None}

        val f1 = retry(f, Seq(1.seconds, 5.seconds, 10.seconds, 30.seconds, 60.seconds))

        Await.result(f1, Duration.Inf)

        a.length
    }

  //
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
          .via(rawToCa)
          .filter(_ != None)
          .via(caToInsert)
          .mapConcat(identity)
          .grouped(50)
          .via(batch)
          .via(updateControl)
          .runWith(Sink.head)

        /*
        val r = s
          .via(messageToRaw)
          .filter(_ != null)
          .via(balancer(persistToCassandra,10))
          .log("Persist")
          .grouped(100000)
          .via(logProgress)
          .runWith(Sink.head)
        */

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
