package com.eztier.http.test

import org.scalatest.{FunSpec, Matchers}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Authority, Path}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Host, RawHeader}
import akka.stream.ActorMaterializer

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.Future

object models {
  import akka.http.scaladsl.model.{HttpProtocol, HttpProtocols}

  case class Target(scheme: String, host: String, port: Int, weight: Int = 1, protocol: HttpProtocol = HttpProtocols.`HTTP/1.1`) {
    def url: String = s"$scheme://$host:$port"
  }

  object Target {
    def apply(url: String): Target = {
      url.split("://|:").toList match {
        case scheme :: host :: port :: Nil => Target(scheme, host, port.toInt)
        case _ => throw new RuntimeException(s"Bad target: $url")
      }
    }

    def weighted(url: String, weight: Int): Target = {
      Target(url).copy(weight = weight)
    }

    def proto(url: String, protocol: HttpProtocol): Target = {
      Target(url).copy(protocol = protocol)
    }

    def protoWeight(url: String, weight: Int, protocol: HttpProtocol): Target = {
      Target(url).copy(weight = weight, protocol = protocol)
    }
  }
}


class TestProxySpec extends FunSpec with Matchers with ScalatestRouteTest {

  describe ("Proxy Suite") {

    import models._

    def NotFound(path: String) = HttpResponse(
      404,
      entity = HttpEntity(ContentTypes.`application/json`, Json.obj("error" -> Json.fromString(s"$path not found")).noSpaces)
    )

    val services: Map[String, Target] = Map(
      "solr" -> Target("http://localhost:8983")
    )

    def extractHost(request: HttpRequest): String = request.header[Host].map(_.host.address()).getOrElse("--")

    it("Should forward request") {

      implicit val system = ActorSystem()
      implicit val executor = system.dispatcher
      implicit val materializer = ActorMaterializer()
      implicit val http = Http(system)

      def handler(request: HttpRequest, formData: Option[FormData] = None) = {
        val solr = "solr"

        services.get(solr) match {
          case Some(target) => {
            val headersIn: Seq[HttpHeader] =
              request.headers.filterNot(t => t.name() == "Host") :+
                Host(target.host) :+
                RawHeader("X-Fowarded-Host", solr) :+
                RawHeader("X-Fowarded-Scheme", request.uri.scheme)

            val a = request.method // HttpMethods.POST

            val proxyRequest = request.copy(
              uri = request.uri.copy(
                scheme = target.scheme,
                authority = Authority(host = Uri.NamedHost(target.host), port = target.port),
                path = Path(request.uri.path.toString().replace("search", solr))
              ),
              headers = headersIn.toList,
              entity = formData match {
                case Some(a)  => a.toEntity
                case _ => request.entity
              }
            )
            val pr = proxyRequest
            http.singleRequest(proxyRequest)
          }
          case None => Future.successful(NotFound(solr))
        }
      }

      val proxy = Route {
        path("search" / Remaining) { remaining =>
          get { context =>
            val request = context.request

            val f = handler(request)

            context.complete(f)

          }
        }
      }

      // q=*:*&rows=1
      val proxy2 = extractRequest {
        request =>
          path("search" / Remaining) { remaining =>
            post {
              formField("suggest") { (suggest) => {
                  val mod = suggest.split(' ').map("suggest:"+_).mkString(" AND ")
                  val fd = FormData("q" -> mod, "rows" -> "10")

                  val f = handler(request, Some(fd))

                  complete(f)
                }
              }

            }
          }
      }

      Get("/search/patient/select?q=city%3Abuen%20AND%20street%3A\"men%20st\"") ~> proxy ~> check {
        val r = responseAs[String]

        r.length should be > (0)
      }

      // miny should return Mouse, Minnie and mike should return Mouse, Mickey
      Post("/search/patient/select", FormData("suggest" -> "miny 1928* 135*")) ~> proxy2 ~> check {
        val r = responseAs[String]

        r.length should be > (0)
      }

    }

  }

}
