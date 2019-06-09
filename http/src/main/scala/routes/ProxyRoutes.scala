package com.eztier.rest.routes

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Authority, Path}
import akka.http.scaladsl.model.{ContentTypes, FormData, HttpEntity, HttpHeader, HttpProtocol, HttpProtocols, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.headers.{Host, RawHeader}
import akka.http.scaladsl.server.Directives.{complete, extractRequest, formField, formFieldMap, get, path, post}
import akka.http.scaladsl.server._
import Directives._
import com.eztier.rest.WebServer._
import io.circe.Json

import scala.concurrent.Future

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
}

trait ProxyRoutes {
  import ProxyModels._

  lazy val httpProxyRoute = proxy
  lazy val httpProxyRoute2 = proxy2
  lazy val httpProxyRoute3 = proxy3

  def NotFound(path: String) = HttpResponse(
    404,
    entity = HttpEntity(ContentTypes.`application/json`, Json.obj("error" -> Json.fromString(s"$path not found")).noSpaces)
  )

  val services: Map[String, Target] = Map(
    "solr" -> Target("http://localhost:8983")
  )

  def extractHost(request: HttpRequest): String = request.header[Host].map(_.host.address()).getOrElse("--")

  def handler(request: HttpRequest, formData: Option[FormData] = None) : Future[HttpResponse] = {
    implicit val http = Http(actorSystem)

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
      withExecutionContext(Routes.blockingDispatcher) {
        get { context =>
          val request = context.request

          val f = handler(request)

          context.complete(f)
        }
      }
    }
  }

  // q=*:*&rows=1
  val proxy2 = extractRequest {
    request =>
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

  val proxy3 = extractRequest { request =>
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
