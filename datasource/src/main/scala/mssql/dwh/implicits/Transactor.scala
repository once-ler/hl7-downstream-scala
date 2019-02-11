package com.eztier.datasource.mssql.dwh.implicits

import doobie._
import cats.effect.IO
import com.eztier.common.Configuration._

import com.eztier.datasource.common.implicits.ExecutionContext._

object Transactors {
  val url = conf.getString(s"$env.doobie.mssql.execution_log.url")
  val driver = conf.getString(s"$env.doobie.mssql.execution_log.driver")
  val user = conf.getString(s"$env.doobie.mssql.execution_log.user")
  val pass = conf.getString(s"$env.doobie.mssql.execution_log.password")

  implicit val cs = IO.contextShift(ec)

  implicit lazy val xa = Transactor.fromDriverManager[IO](
    driver, url, user, pass
  )
}
