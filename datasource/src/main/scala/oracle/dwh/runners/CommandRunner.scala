package com.eztier.datasource.oracle.dwh.runners

import akka.stream.scaladsl.Source
import java.time.LocalDateTime
import scala.reflect.runtime.universe._

import com.eztier.datasource.oracle.dwh.implicits._
import com.eztier.datasource.oracle.dwh.implicits.Transactors._
import com.eztier.datasource.common.runners.CommandRunner._

object CommandRunner {
  def search[A](fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "HR")
               (implicit searchable: Searchable[A], typeTag: TypeTag[A]): Source[A, akka.NotUsed] = {

    val io = searchable.search(fromDateTime, toDateTime, schema)

    val src = tryRunIO(io)

    Source(src)
  }
}