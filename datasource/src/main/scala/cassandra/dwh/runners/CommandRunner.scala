package com.eztier.datasource.cassandra.dwh.runners

import java.time.LocalDateTime

import scala.concurrent.Future
import com.datastax.driver.core.ResultSet
import com.eztier.datasource.cassandra.dwh.implicits._
import com.eztier.hl7mock.{CaBase, CaControl}

object CommandRunner {
  def update[A <: CaBase, B <: CaControl](msg: String)(implicit updatable: Updatable[A, B]): Future[Int] = {
    updatable.update(msg)
  }

  def updateDate[A](dt: LocalDateTime)(implicit dateUpdatable: DateUpdatable[A]): Future[ResultSet] = {
    dateUpdatable.update(dt)
  }

  def search[A <: CaBase, B <: CaControl](stmt: String)(implicit searchable: Searchable[A, B]): Future[ResultSet] = {
    searchable.search(stmt)
  }
}
