package com.eztier.datasource.mssql.dwh.implicits

import cats.effect.{IO, Resource}
import doobie._
import doobie.implicits._
import java.time.LocalDateTime
import com.eztier.datasource.mssql.dwh.implicits.Transactors._
import com.eztier.datasource.common.models.ExecutionLog
import com.eztier.datasource.common.models.ExecutionLogImplicits._

trait Searchable[A] {
  def search(toStore: String, fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "ril")(implicit xa: Transactor[IO]): IO[List[A]]
}

trait AdHocable[A] {
  def adhoc(sqlstring: String)(implicit xa: Transactor[IO]): IO[List[A]]
}

object Searchable {
  implicit object ExecutionLogSearch extends Searchable[ExecutionLog] {
    // val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    // fromDateTime.format(formatter)

    override def search(toStore: String, fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "ril")(implicit xa: Transactor[IO]): IO[List[ExecutionLog]] = {
      val stmt = fr"""select 
        start_time StartTime, 
        from_store FromStore,
        to_store ToStore,
        study_id StudyId,
        wsi WSI,
        caller Caller,
        request Request,
        response Response,
        error Error
        from """ ++ 
        Fragment(schema, List()) ++ fr".wsi_execution_hist where to_store = " ++ 
        Fragment(s"'$toStore'", List()) ++ fr" and start_time >= " ++
        Fragment(s"'${fromDateTime.toString()}'", List()) ++ fr" and start_time <= " ++
        Fragment(s"'${toDateTime.toString()}'", List())

      stmt
        .query[ExecutionLog]
        .stream
        .compile
        .to[List]
        .transact(xa)
    }
  }

}