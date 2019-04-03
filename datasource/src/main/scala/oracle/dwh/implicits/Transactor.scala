package com.eztier.datasource.oracle.dwh.implicits

import doobie._
import cats.effect.IO
import com.eztier.common.Configuration._

import com.eztier.datasource.common.implicits.ExecutionContext._

object Transactors {
  val url = conf.getString(s"$env.doobie.oracle.specimen.url")
  val driver = conf.getString(s"$env.doobie.oracle.specimen.driver")
  val user = conf.getString(s"$env.doobie.oracle.specimen.user")
  val pass = conf.getString(s"$env.doobie.oracle.specimen.password")

  implicit val cs = IO.contextShift(ec)

  implicit lazy val xa = Transactor.fromDriverManager[IO](
    driver, url, user, pass
  )
}
