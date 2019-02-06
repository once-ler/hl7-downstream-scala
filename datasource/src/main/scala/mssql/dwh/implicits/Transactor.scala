package com.eztier.datasource.mssql.dwh.implicits

import doobie._
import doobie.implicits._
import cats.effect.IO

import com.eztier.datasource.common.implicits.ExecutionContext._

object Transactors {
  val env = if (System.getenv("SCALA_ENV") == null) "development" else System.getenv("SCALA_ENV")

  import com.typesafe.config.ConfigFactory
  val conf = ConfigFactory.load()

  val url = conf.getString(s"$env.doobie.mssql.execution_log.url")
  val driver = conf.getString(s"$env.doobie.mssql.execution_log.driver")
  val user = conf.getString(s"$env.doobie.mssql.execution_log.user")
  val pass = conf.getString(s"$env.doobie.mssql.execution_log.password")

  implicit val cs = IO.contextShift(ec)

  implicit lazy val xa = Transactor.fromDriverManager[IO](
    driver, url, user, pass
  )
}