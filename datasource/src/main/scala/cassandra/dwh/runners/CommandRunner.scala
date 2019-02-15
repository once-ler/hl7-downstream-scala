package com.eztier.datasource.cassandra.dwh.runners

import com.datastax.driver.core.ResultSet

import scala.concurrent.Future
import com.eztier.datasource.cassandra.dwh.implicits.{Searchable, Updatable}

object CommandRunner {
  def update[A, B](msg: String)(implicit updatable: Updatable[A, B]): Future[Int] = {
    updatable.update(msg)
  }

  def search[A, B](stmt: String)(implicit searchable: Searchable[A, B]): Future[ResultSet] = {
    searchable.search(stmt)
  }
}
