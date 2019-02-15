package com.eztier.datasource.mssql.dwh.runners

import akka.stream.scaladsl.Source
import java.time.LocalDateTime
import scala.reflect.runtime.universe._

import com.eztier.datasource.mssql.dwh.implicits._
import com.eztier.datasource.mssql.dwh.implicits.Transactors._
import com.eztier.datasource.common.runners.CommandRunner._

object CommandRunner {
  def search[A](toStore: String, fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "ril")
    (implicit searchable: Searchable[A], typeTag: TypeTag[A]): Source[A, akka.NotUsed] = {
  
    val io = searchable.search(toStore, fromDateTime, toDateTime, schema)

    val src = tryRunIO(io)

    Source(src)
  }
}