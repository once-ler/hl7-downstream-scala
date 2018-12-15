package com.eztier.test

import org.scalatest.{BeforeAndAfter, Failed, FunSpec, Matchers}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.eztier.postgres.eventstore.runners.CommandRunner
import com.eztier.postgres.eventstore.models.{Patient, GenericEvent, AppendEventResult}
import com.eztier.hl7mock.types.CaPatient

import scala.concurrent.Await
import scala.concurrent.duration._

// testOnly *TestDoobieSpec
// to run only the tests whose name includes the substring "foo". -z foo
// Exact match -t foo i.e.  testOnly *TestDoobieSpec -- -t foo
class TestDoobieSpec extends FunSpec with Matchers {
  implicit val system = ActorSystem("Sys")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val ev = GenericEvent("test::generic::event", s"""{"id":"foo"}""")
  val h = CommandRunner.addEvent(List(ev))
    .runWith(Sink.seq)

  val r2 = Await.result(h, 500 millis)

  r2.foreach(println(_))
/**
  val g = CommandRunner.adhoc[CaPatient]("select current from hl7.patient limit(1)")
    .runWith(Sink.seq)

  val r1 = Await.result(g, 500 millis)

  r1.foreach(println(_))

  val f = CommandRunner.search[Patient]("c")
    .runWith(Sink.seq)

  val r = Await.result(f, 500 millis)

  r.foreach(println(_))

  println("Done")

  val f2 = CommandRunner.search[CaPatient]("a")
    .runWith(Sink.seq)

  val r2 = Await.result(f2, 500 millis)

  r2.foreach(println(_))
**/
}
