package com.eztier.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.eztier.common.Configuration._
import com.eztier.rest.routes.{Routes}

// object WebServer extends App with SearchStreamRoutes with StaticRoutes with Hl7StreamRoutes {
object WebServer extends App {
  implicit val actorSystem = ActorSystem(name = "http-actor-system")
  implicit val streamMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val logger = actorSystem.log

  val bindAddress = conf.getString(s"$env.bind-address")
  val port = conf.getInt(s"$env.port")

  val bindingFuture = Http().bindAndHandle(Routes.allRoutes, bindAddress, port)
  bindingFuture
    .map(_.localAddress)
    .map(addr => s"Bound to $addr")
    .foreach(logger.info)
}
