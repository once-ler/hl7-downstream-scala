package com.eztier.rest.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
// import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
// import ContentTypeResolver.Default
import akka.util.ByteString

// akka execution context
import akka.stream.scaladsl.{Flow, Source, Sink}
import akka.stream.{ActorMaterializer, ThrottleMode}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

// akka-http-circe
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

// doobie
import com.eztier.datasource.common.models.{Patient, Model, ExecutionLogRange}
import com.eztier.datasource.postgres.eventstore.runners.CommandRunner

// Marshalled types
import com.eztier.hl7mock.types.CaPatient
import com.eztier.datasource.common.models.ExecutionLogMini

// Unmarshaller for circe
import com.eztier.datasource.postgres.eventstore.models.CaPatientImplicits._

// For testing
case class Dummy(name: String)

trait SearchStreamRoutes {
  implicit val actorSystem: ActorSystem
  implicit val streamMaterializer: ActorMaterializer
  implicit val executionContext: ExecutionContext

  lazy val httpStreamingRoutes = streamingJsonRoute
  lazy val httpInfoStreamingRoutes = streamingInfoRoute
  lazy val httpStreamingSearchRoutes = streamingSearchRoute
  lazy val httpStreamingSearchLogRoutes = streamingSearchLogRoute

  implicit val jsonStreamingSupport: akka.http.scaladsl.common.JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def streamingInfoRoute =
    path("info") {
      get {
        val sourceOfNumbers = Source(1 to 15)
        val byteStringSource =
          sourceOfNumbers.map(num => s"mrn:$num")
            // .throttle(elements = 100, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
            .map(_.toString)
            .map(s => ByteString(s))

        complete(HttpEntity(`text/plain(UTF-8)`, byteStringSource))
      }
    }

  /*
    @test
      for i in {1..10000}; do curl localhost:9000/streaming-json & done
  */
  def streamingJsonRoute =
    path("streaming-json") {
      val newline = ByteString("\n")

      implicit val jsonStreamingSupport = EntityStreamingSupport.json()
        .withFramingRenderer(Flow[ByteString].map(bs => bs ++ newline))

      get {
        val sourceOfNumbers = Source(1 to 15)
        val sourceOfSearchMessages =
          sourceOfNumbers.map(num => Patient(s"name:$num"))
            // .throttle(elements = 100, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)

        complete(sourceOfSearchMessages)
      }
    }
    
  /*
    @test
      curl -XPOST -H 'Content-Type:application/json'  -d '{"name": "abc"}' localhost:9000/search
  */
  def streamingSearchRoute = {
    path("search") {
      get {
        val sourceOfNumbers = Source(1 to 15)
        val sourceOfSearchMessages =
          sourceOfNumbers.map(num => Dummy(s"name:$num"))
            // .throttle(elements = 100, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)

        complete(sourceOfSearchMessages)
      } ~ post {
        entity(as[Patient]) { p =>
          val resp = CommandRunner
            .search[CaPatient](p.name)
            // .throttle(elements = 100, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
            
          complete(resp)
        }
      }
    }

  }

  /*
    @test
    curl -XPOST -H 'Content-Type:application/json'  -d '{"toStore": "store_def", "fromDateTime": "2019-01-31T12:43:03.141", "toDateTime": "2019-02-06T18:58:50.141"}' localhost:9000/log/wsi?stream=1
    curl -XPOST -H 'Content-Type:application/json'  -d '{"toStore": "store_def", "fromDateTime": "2019-01-31T12:43:03.141", "toDateTime": "2019-02-06T18:58:50.141"}' localhost:9000/log/wsi?format=html
  
  */
  def streamingSearchLogRoute = {

    path("log" / "wsi") {
      post {
        entity(as[ExecutionLogRange]) { p =>
          
          val resp = CommandRunner
            .searchLog[ExecutionLogMini](p.toStore, p.fromDateTime, p.toDateTime)
            // .throttle(elements = 100, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
          
          parameters('stream.?, 'format.?) { (stream, format) =>
            format match {
              case Some(a) if a == "html" =>
                
                val html = resp.zipWithIndex.map{ t =>
                  val a = t._1
                  val i = t._2
                  
                  ByteString(s"""<tr>
                  |<td>${i.toString()}</td>
                  |<td>${a.StartTime.toString()}</td>
                  |<td>${a.FromStore}</td>
                  |<td>${a.ToStore}</td>
                  |<td>${a.StudyId}</td>
                  |<td">${a.WSI}</td>
                  |<td">${a.Caller}</td>
                  |<td">${a.Response}</td>
                  |</tr>""".stripMargin.replaceAll("\n", "") + "\n")
                }

                complete(HttpEntity(`text/csv(UTF-8)`, html))
              case _ =>
                stream match {
                  case Some(a) =>
                    if (a == "1") {
                      val newline = ByteString("\n")
                      implicit val jsonStreamingSupport = EntityStreamingSupport.json()
                        .withFramingRenderer(Flow[ByteString].map(bs => bs ++ newline))

                      complete(resp)
                    } else complete(resp)
                  case _ => complete(resp)
                }
            }
          }
        }

      }
    }
  }

}
