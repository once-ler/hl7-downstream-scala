package com.eztier.datasource.postgres.eventstore.implicits

import java.sql.Connection

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

  /*
  private[this] var maybeHikariTransactor: Option[Resource[IO, HikariTransactor[IO]]] = None

  implicit val hikariTransactor: Resource[IO, HikariTransactor[IO]] =
    if (maybeHikariTransactor == None) {
      maybeHikariTransactor = Some(
        for {
          ce <- ExecutionContexts.fixedThreadPool[IO](poolSize) // connect EC
          te <- ExecutionContexts.cachedThreadPool[IO] // transaction EC
          xa <- HikariTransactor.newHikariTransactor[IO](
            driver,
            url,
            user,
            pass,
            ce,
            te
          )
        } yield xa
      )
      maybeHikariTransactor.get
    } else {
      maybeHikariTransactor.get
    }

  */

  implicit val hikariTransactor: Resource[IO, HikariTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](poolSize) // connect EC
    te <- ExecutionContexts.cachedThreadPool[IO] // transaction EC
    xa <- HikariTransactor.newHikariTransactor[IO](
      driver,
      url,
      user,
      pass,
      ce,
      te
    )
  } yield xa


  val config = new HikariConfig()
  config.setDriverClassName(driver)
  config.setJdbcUrl(url)
  config.setUsername(user)
  config.setPassword(pass)
  config.setMaximumPoolSize(poolSize)

  val xa1: IO[HikariTransactor[IO]] =
    IO.pure(HikariTransactor.apply[IO](new HikariDataSource(config), ec, ec))

  val ds = new HikariDataSource(config)

  val xa: Transactor[IO] = Transactor.fromDataSource[IO](ds, ec, ec)

  val xa2: Transactor[IO] = Transactor.fromConnection[IO](ds.getConnection, ec)


}
