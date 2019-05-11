package com.eztier.rest.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, ZipWith}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.ByteString
import java.io.{PrintWriter, StringWriter}

import akka.event.LoggingAdapter
import ca.uhn.hl7v2.{DefaultHapiContext, HL7Exception}
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.parser.{CanonicalModelClassFactory, EncodingNotSupportedException}
import ca.uhn.hl7v2.validation.impl.NoValidation

import scala.concurrent.ExecutionContext

object Hapi {
  private val pipeParser = {
    val hapiContext = new DefaultHapiContext()
    hapiContext.setModelClassFactory(new CanonicalModelClassFactory("2.3.1"))
    hapiContext.setValidationContext(new NoValidation)
    hapiContext.getPipeParser()
  }

  def parseMessage(in: String)(implicit logger: LoggingAdapter): Option[Message] =
    try {
      val a = pipeParser.parse(in)
      Some(a)
    } catch {
      case e: EncodingNotSupportedException => {
        val sw = new StringWriter
        e.printStackTrace(new PrintWriter(sw))
        logger.error(sw.toString)
        None
      }
      case e1: HL7Exception => {
        val sw = new StringWriter
        e1.printStackTrace(new PrintWriter(sw))
        logger.error(sw.toString)
        None
      }
    }
}

trait Hl7StreamRoutes {
  // implicit val actorSystem: ActorSystem
  // implicit val streamMaterializer: ActorMaterializer
  implicit val logger: LoggingAdapter

  lazy val httpHl7StreamingRoutes = streamingHl7Route
  lazy val httpHl7AlternateStreamingRoutes = streamingHl7AlternateRoute

  def generateAck = Flow[String].map {
    s =>
      Hapi.parseMessage(s) match {
        case Some(m) => m.generateACK().encode()
        case _ => "Error"
      }
  }

  def persist = Flow[String].map { s => 
    Thread.sleep(500)
    1 
  }

  def persistAndGenerateAck = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

      val bcast = b.add(Broadcast[String](2))
      val zip = b.add(ZipWith[String, Int, String]((ack: String, count: Int) => ack))

      bcast ~> generateAck ~> zip.in0
      bcast ~> persist ~> zip.in1

      FlowShape(bcast.in, zip.out)
    }

  val persistMethod =
    post {
      entity(as[String]) { rawString =>

        val resp = Source.single(rawString)
          .via(persistAndGenerateAck)
          .log("ACK")
          .map(s => ByteString(s))

        extractExecutionContext { implicit executor =>
          complete(HttpEntity(`text/plain(UTF-8)`, resp))
        }

      }
    }

  def streamingHl7Route = path("hl7") {
    import com.eztier.rest.WebServer
    val blockingDispatcher = WebServer.actorSystem.dispatchers.lookup("blocking-dispatcher")

    persistMethod
  }

  // https://doc.akka.io/docs/akka-http/current/routing-dsl/path-matchers.html
  def streamingHl7AlternateRoute = path("dump" / Segments) {
    import com.eztier.rest.WebServer
    implicit val blockingDispatcher = WebServer.actorSystem.dispatchers.lookup("blocking-dispatcher")

    l => persistMethod
  }

}
