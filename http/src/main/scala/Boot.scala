package com.eztier.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._

import com.eztier.common.Configuration._
import com.eztier.rest.routes.SearchStreamRoutes
import com.eztier.rest.routes.StaticRoutes

object Boot extends App with SearchStreamRoutes with StaticRoutes {
  implicit val actorSystem = ActorSystem(name = "http-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val logger = actorSystem.log

  val bindAddress = conf.getString(s"$env.bind-address")
  val port = conf.getInt(s"$env.port")

  val allRoutes =
    httpStreamingRoutes ~ 
      httpInfoStreamingRoutes ~ 
        httpStreamingSearchRoutes ~
          httpStreamSearchAggregationLogRoutes ~
            httpStreamingSearchLogRoutes ~
              httpStaticRoutes ~
                httpPublicRoutes ~
                  httpApiRoutes

  val bindingFuture = Http().bindAndHandle(allRoutes, bindAddress, port)
  bindingFuture
    .map(_.localAddress)
    .map(addr => s"Bound to $addr")
    .foreach(logger.info)
}
