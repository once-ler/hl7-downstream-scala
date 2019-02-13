package com.eztier.datasource.common.models

import java.time.LocalDate

// circe unmarshalling DateTime support
import io.circe.generic.semiauto._
import io.circe.java8.time._

case class ExecutionAggregationLog(symbol: String, date: LocalDate, price: Long)

object ExecutionAggregationLog {
  implicit val AEncoder = deriveEncoder[ExecutionAggregationLog]
  implicit val ADecoder = deriveDecoder[ExecutionAggregationLog]
}
