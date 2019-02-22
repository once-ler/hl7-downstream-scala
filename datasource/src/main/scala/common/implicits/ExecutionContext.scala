package com.eztier.datasource.common.implicits

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object ExecutionContext {
  implicit val system = ActorSystem("datasource-actor-system")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val logger = system.log
}
