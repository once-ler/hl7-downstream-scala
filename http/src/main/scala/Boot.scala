package com.eztier.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import com.eztier.rest.routes.SearchStreamRoutes
import com.eztier.rest.routes.StaticRoutes

object Boot extends App with SearchStreamRoutes with StaticRoutes {
  implicit val actorSystem = ActorSystem(name = "http-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val logger = actorSystem.log
  val allRoutes = 
    httpStreamingRoutes ~ 
      httpInfoStreamingRoutes ~ 
        httpStreamingSearchRoutes ~ 
          httpStreamingSearchLogRoutes
            httpStaticRoutes ~
              httpPublicRoutes ~
                httpApiRoutes

  val bindingFuture = Http().bindAndHandle(allRoutes, "localhost", 9000)
  bindingFuture
    .map(_.localAddress)
    .map(addr => s"Bound to $addr")
    .foreach(logger.info)
}
