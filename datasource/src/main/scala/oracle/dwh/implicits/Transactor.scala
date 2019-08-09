package com.eztier.datasource.oracle.dwh.implicits

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext
import doobie._
import cats.effect.{IO}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import com.eztier.common.Configuration._
import com.eztier.datasource.common.implicits.ExecutionContext._

object Transactors {
  val url = conf.getString(s"$env.doobie.oracle.specimen.url")
  val driver = conf.getString(s"$env.doobie.oracle.specimen.driver")
  val user = conf.getString(s"$env.doobie.oracle.specimen.user")
  val pass = conf.getString(s"$env.doobie.oracle.specimen.password")
  var poolSize = if (conf.hasPath(s"$env.doobie.oracle.specimen.pool-size")) conf.getInt(s"$env.doobie.oracle.specimen.pool-size") else 25

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
