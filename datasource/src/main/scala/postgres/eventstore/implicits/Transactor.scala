package com.eztier.datasource.postgres.eventstore.implicits

import doobie._
import cats.effect.{IO, Resource}
import com.eztier.common.Configuration._
import com.eztier.datasource.common.implicits.ExecutionContext._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor

object Transactors {
  val url = conf.getString(s"$env.doobie.postgres.patient.url")
  val driver = conf.getString(s"$env.doobie.postgres.patient.driver")
  val user = conf.getString(s"$env.doobie.postgres.patient.user")
  val pass = conf.getString(s"$env.doobie.postgres.patient.password")
  var poolSize = if (conf.hasPath(s"$env.doobie.postgres.patient.pool-size")) conf.getInt(s"$env.doobie.postgres.patient.pool-size") else 25

  implicit val cs = IO.contextShift(ec)

  implicit lazy val xa = Transactor.fromDriverManager[IO](
    driver, url, user, pass
  )

  val hikariTransactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](poolSize) // connect EC
      te <- ExecutionContexts.cachedThreadPool[IO] // transaction EC
      xa1 <- HikariTransactor.newHikariTransactor[IO](
        driver,
        url,
        user,
        pass,
        ce,
        te
      )
    } yield xa1

  /*
  val config = new HikariConfig()
  config.setDriverClassName(driver)
  config.setJdbcUrl(url)
  config.setUsername(user)
  config.setPassword(pass)
  config.setMaximumPoolSize(poolSize)

  val DbTransactor: IO[HikariTransactor[IO]] =
    IO.pure(HikariTransactor.apply[IO](new HikariDataSource(config), ec, ec))
  */

}
