package com.eztier.http.test

import java.net.InetSocketAddress

import org.scalatest.{FunSpec, Matchers}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.http.scaladsl.server._
import Directives._
import akka.actor.ActorSystem
import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.model.Uri.{Authority, Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Host, RawHeader}
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.stream.ActorMaterializer
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

// second.dilated
import akka.testkit._
import scala.concurrent.Future
// import scala.concurrent.duration.DurationInt
import scala.concurrent.duration._

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
      "solr" -> Target("http://localhost:8983"),
      "reddit" -> Target("https://www.reddit.com:443"),
      "pubmed" -> Target("https://eutils.ncbi.nlm.nih.gov:443")
    )

    def extractHost(request: HttpRequest): String = request.header[Host].map(_.host.address()).getOrElse("--")

    it("Should forward request") {

      implicit val system = ActorSystem()
      implicit val executor = system.dispatcher
      implicit val materializer = ActorMaterializer()
      implicit val http = Http(system)

      // To expand Route test timeout
      // Or application.conf
      // akka.test.timefator=5.0
      implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(5).second.dilated(system))

      def handler(request: HttpRequest, formData: Option[FormData] = None, service: String = "solr") : Future[HttpResponse] = {

        services.get(service) match {
          case Some(target) => {
            val headersIn: Seq[HttpHeader] =
              request.headers.filterNot(t => t.name() == "Host") :+
                Host(target.host) :+
                RawHeader("X-Fowarded-Host", service) :+
                RawHeader("X-Fowarded-Scheme", request.uri.scheme)

            val b = request.uri.path.toString()

            val proxyRequest = request.copy(
              uri = request.uri.copy(
                scheme = target.scheme,
                authority = Authority(host = Uri.NamedHost(target.host), port = target.port),
                path = service match {
                  case "solr" =>
                    Path(b.replace("search", service))
                  case _ =>
                    Path(b.replace(s"/api/$service", ""))
                }
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
          case None => Future.successful(NotFound(service))
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

      val proxy3 = extractRequest {
        request =>
          path("search" / Remaining) { remaining =>
            post {
              formFieldMap { fields =>
                val mod = fields.map(a => s"${a._1}:${a._2}").mkString(" AND ")
                val fd = FormData("q" -> mod, "rows" -> "10")

                val f = handler(request, Some(fd))

                complete(f)
              }
            }
          }
        }

      val proxy4 = Route {
        path("api" / Segment / Remaining) { (segment, remaining) =>
          get { context =>
            val request = context.request

            val f = handler(request, None, segment)

            context.complete(f)
          }
        }
      }

      val topic = "spiderman far from home"
      val qb = Query.newBuilder

      qb += (
        "q" -> s"title:$topic",
        "syntax" -> "plain",
        "restrict_sr" -> "false",
        "include_facets" -> "false",
        "limit" -> "10",
        "sr_detail"->"false"
      )

      val qs = qb.result()

      val a = qs.value

      val uri0 = Uri("/api/reddit/search.json").withQuery(qs)

      // Get("/api/reddit?q=subreddit:spiderman%20far%20from%20home&syntax=plain&restrict_sr=true&include_facets=false&limit=10&sr_detail=false") ~> proxy ~> check {
      Get(uri0.toString()) ~> proxy4 ~> check {
        val r = responseAs[String]

        r.length should be > (0)
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

      Post("/search/interface_logging/select", FormData("from_store" -> "epic", "study_id" -> "ABC*")) ~> proxy3 ~> check {
        val r = responseAs[String]

        r.length should be > (0)
      }

    }

  }

}
