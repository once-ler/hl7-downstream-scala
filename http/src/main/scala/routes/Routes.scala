package com.eztier.rest.routes

import akka.http.scaladsl.server.Directives._

// import com.eztier.rest.WebServer.httpStreamingRoutes

// import com.eztier.rest.routes.SearchStreamRoutes
// import com.eztier.rest.routes.StaticRoutes
// import com.eztier.rest.routes.Hl7StreamRoutes
// import com.eztier.rest.routes.ProxyRoutes

object Routes extends SearchStreamRoutes with StaticRoutes with Hl7StreamRoutes with ProxyRoutes {
  import com.eztier.rest.WebServer
  val blockingDispatcher = WebServer.actorSystem.dispatchers.lookup("blocking-dispatcher")

  implicit val logger = WebServer.logger

  val allRoutes =
    httpStreamingRoutes ~
      httpInfoStreamingRoutes ~
        httpStreamingSearchRoutes ~
          httpStreamSearchAggregationLogRoutes ~
            httpStreamingSearchLogRoutes ~
              httpStaticRoutes ~
                httpPublicRoutes ~
                  httpApiRoutes ~
                    httpHl7StreamingRoutes ~
                      httpHl7AlternateStreamingRoutes ~
                        httpProxyRoute ~
                          httpProxyRoute2 ~
                            httpProxyRoute3 ~
                              httpProxyRoute4
}
