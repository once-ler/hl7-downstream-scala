package com.eztier.datasource.mssql.dwh.implicits

import cats.effect.IO
import doobie._
import doobie.implicits._
import org.joda.time.DateTime

import com.eztier.datasource.common.models._
import com.eztier.datasource.mssql.dwh.implicits.Transactors._
import com.eztier.datasource.mssql.dwh.models.ExecutionLog

trait Searchable[A] {
  def search(toStore: String, fromDateTime: DateTime, toDateTime: DateTime, schema: String = "ril")(implicit xa: Transactor[IO]): IO[List[A]]
}

trait AdHocable[A] {
  def adhoc(sqlstring: String)(implicit xa: Transactor[IO]): IO[List[A]]
}

object Searchable {
  implicit object ExecutionLogSearch extends Searchable[ExecutionLog] {
    override def search(toStore: String, fromDateTime: DateTime, toDateTime: DateTime, schema: String = "ril")(implicit xa: Transactor[IO]): IO[List[ExecutionLog]] = {
      val stmt = fr"select start_time StartTime from " ++ 
        Fragment(schema, None) ++ fr".wsi_execution_hist where to_store = " ++ 
        Fragment(s"'$toStore'", None) ++ fr" and start_time >= " ++
        Fragment(s"'${fromDateTime.toLocalDateTime.toString()}'", None) ++ fr" and start_time <= " ++
        Fragment(s"'${toDateTime.toLocalDateTime.toString()}'", None)
      
      stmt
        .query[ExecutionLog]
        .stream
        .compile
        .to[List]
        .transact(xa)
    }
  }

}