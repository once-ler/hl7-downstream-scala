package com.eztier.rest.routes

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes

import scala.concurrent.ExecutionContext

trait StaticRoutes {
  implicit val actorSystem: ActorSystem
  implicit val streamMaterializer: ActorMaterializer
  implicit val executionContext: ExecutionContext

  lazy val httpStaticRoutes = staticRoutes
  lazy val httpPublicRoutes = publicRoutes
  lazy val httpApiRoutes = apiRoutes
  
  val workingDirectory = System.getProperty("user.dir")

  def resourcesDir(pathMatcher : String, dir : String) : Route = 
    pathPrefix(pathMatcher) {
      pathEndOrSingleSlash {
        getFromFile(dir + "/index.html")
      } ~ 
      getFromDirectory(dir)
    }

  val staticRoutes = resourcesDir("static", workingDirectory + "/static") 
  val publicRoutes = resourcesDir("public", workingDirectory + "/public") 
  val apiRoutes = resourcesDir("api", workingDirectory + "/api") 

}

