package com.eztier.rest.routes

import java.nio.charset.StandardCharsets

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Authority, Path}
import akka.http.scaladsl.model.{ContentTypes, FormData, HttpEntity, HttpHeader, HttpProtocol, HttpProtocols, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.headers.{Host, RawHeader}
import akka.http.scaladsl.server.Directives.{complete, extractRequest, formField, formFieldMap, get, path, post}
import akka.http.scaladsl.server._
// Collects all default directives into one trait for simple importing.
import Directives._
import io.circe.Json
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import scala.concurrent.Future

import com.eztier.rest.WebServer._
import com.eztier.common.Configuration.{env, conf}

object ProxyModels {
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
  }

  object SolrCloud {
    val cfg = conf.getConfig(s"$env.solr")
    val url = cfg.getString("url")
    val user = cfg.getString("user")
    val password = cfg.getString("password")
    val authorizationHeader = RawHeader("authorization",
      java.util.Base64.getEncoder.encodeToString(s"Basic ${user}:${password}".getBytes(StandardCharsets.UTF_8)))
  }
}

trait ProxyRoutes {
  import ProxyModels._

  lazy val httpProxyRoute = proxy
  lazy val httpProxyRoute2 = proxy2
  lazy val httpProxyRoute3 = proxy3
  lazy val httpProxyRoute4 = proxy4

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

  implicit val http = Http(actorSystem)

  def handler(request: HttpRequest, formData: Option[FormData] = None, service: String = "solr") : Future[HttpResponse] = {

    services.get(service) match {
      case Some(target) => {
        val solrHeaders: Seq[HttpHeader] =
          service match {
            case "solr" => Seq(SolrCloud.authorizationHeader)
            case _ => Seq.empty
          }

        val headersForward: Seq[HttpHeader] =
          request.headers.filterNot(t => t.name() == "Host") :+
            Host(target.host) :+
            RawHeader("X-Fowarded-Host", service) :+
            RawHeader("X-Fowarded-Scheme", request.uri.scheme)

        val headersIn = headersForward ++ solrHeaders

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

  val proxy =
    handleRejections(corsRejectionHandler) {
      cors() {
        Route {
          path("search" / Remaining) { remaining =>
            withExecutionContext(Routes.blockingDispatcher) {
              get { context =>
                val request = context.request

                val f = handler(request)

                context.complete(f)
              }
            }
          }
        }
      }
    }

  // q=*:*&rows=1
  val proxy2 =
    handleRejections(corsRejectionHandler) {
      cors() {
        extractRequest { request =>
            path("search" / Remaining) { remaining =>
              withExecutionContext(Routes.blockingDispatcher) {

                post {
                  formField("suggest") { (suggest) => {
                    val mod = suggest.split(' ').map("suggest:" + _).mkString(" AND ")
                    val fd = FormData("q" -> mod, "rows" -> "10")

                    val f = handler(request, Some(fd))

                    complete(f)
                  }
                }
              }
            }
          }
        }
      }
    }

  val proxy3 =
    handleRejections(corsRejectionHandler) {
      cors() {
        extractRequest { request =>
          path("search" / Remaining) { remaining =>
            withExecutionContext(Routes.blockingDispatcher) {
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
        }
      }
    }

  val proxy4 =
    handleRejections(corsRejectionHandler) {
      cors() {
        Route {
          path("api" / Segment / Remaining) { (segment, remaining) =>
            get { context =>
              val request = context.request

              val f = handler(request, None, segment)

              context.complete(f)
            }
          }
        }
      }
    }

}
