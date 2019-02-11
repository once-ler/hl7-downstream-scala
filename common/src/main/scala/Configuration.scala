package com.eztier.common

import com.typesafe.config.ConfigFactory

object Configuration {
  val env = if (System.getenv("SCALA_ENV") == null) "development" else System.getenv("SCALA_ENV")
  val workingDir = System.getProperty("user.dir")

  val confFile = if (env == "development") "application.conf" else s"${workingDir}/config/application-production.conf"
  val conf = ConfigFactory.load(confFile)
}
