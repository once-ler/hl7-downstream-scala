package com.eztier.datasource.postgres.eventstore.implicits

import java.util.concurrent.{ Executors, ExecutorService }
import scala.concurrent.ExecutionContext
import doobie._
import cats.effect.{IO}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import com.eztier.common.Configuration._
import com.eztier.datasource.common.implicits.ExecutionContext._

object Transactors {
  val url = conf.getString(s"$env.doobie.postgres.patient.url")
  val driver = conf.getString(s"$env.doobie.postgres.patient.driver")
  val user = conf.getString(s"$env.doobie.postgres.patient.user")
  val pass = conf.getString(s"$env.doobie.postgres.patient.password")
  var poolSize = if (conf.hasPath(s"$env.doobie.postgres.patient.pool-size")) conf.getInt(s"$env.doobie.postgres.patient.pool-size") else 10

  implicit val cs = IO.contextShift(ec)

  private val config = new HikariConfig()
  config.setDriverClassName(driver)
  config.setJdbcUrl(url)
  config.setUsername(user)
  config.setPassword(pass)
  config.setMaximumPoolSize(poolSize)

  private val ds = new HikariDataSource(config)
  private val fixedPool: ExecutorService = Executors.newFixedThreadPool(poolSize)
  private val ce = ExecutionContext.fromExecutorService(fixedPool)
  private val cachedPool = Executors.newCachedThreadPool()
  private val te = ExecutionContext.fromExecutorService(cachedPool)

  implicit lazy val xa: Transactor[IO] = Transactor.fromDataSource[IO](ds, ce, te)
}
