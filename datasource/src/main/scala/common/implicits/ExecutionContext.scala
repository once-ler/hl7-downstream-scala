package com.eztier.datasource.common.implicits

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object ExecutionContext {
  implicit val system = ActorSystem("Sys")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val logger = system.log
}
