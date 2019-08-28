package com.eztier.common

import java.io.File

import com.typesafe.config.ConfigFactory

object Configuration {
  val env = if (System.getenv("SCALA_ENV") == null) "development" else System.getenv("SCALA_ENV")
  val workingDir = System.getProperty("user.dir")

  val tryGetConfig = (fileName: String) => {
    val configFile = new File(s"${workingDir}/config/${fileName}")
    if (configFile.exists()) {
      val config = ConfigFactory.parseFile(configFile)
      ConfigFactory.load(config)
    } else {
      ConfigFactory.load()
    }
  }

  val conf = if (env == "production") {
    tryGetConfig("application-production.conf")
  } else {
    tryGetConfig("application.conf")
  }
}
