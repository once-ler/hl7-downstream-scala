package com.eztier.datasource.mongodb.hl7.runners

import akka.stream.scaladsl.Source
import com.eztier.datasource.mongodb.hl7.implicits.Searchable

object CommandRunner {
  def search[A](from: Long, to: Long)(implicit searchable: Searchable[A]): Source[A, akka.NotUsed] = {
    searchable.search(from, to)
  }
}
