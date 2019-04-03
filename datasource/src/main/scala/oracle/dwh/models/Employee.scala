package com.eztier.datasource.oracle.dwh.models

import doobie.util.Meta
import java.time.LocalDateTime

// circe unmarshalling DateTime support
import io.circe.generic.semiauto._
import io.circe.java8.time._

import scala.math.BigDecimal

case class Employee
(
  EMPLOYEE_ID: Int,
  FIRST_NAME: String,
  LAST_NAME: String,
  JOB_ID: String,
  HIRE_DATE: LocalDateTime,
  SALARY: Option[BigDecimal]
)

object EmployeeImplicits {
  /**
    * imap:
    * def imap[B](f: A => B)(g: B => A): Meta[B]
    ***/
  implicit val LocalDateTimeMeta: Meta[LocalDateTime] =
    Meta[java.sql.Timestamp].imap(_.toLocalDateTime)(java.sql.Timestamp.valueOf)

  implicit val AEncoder = deriveEncoder[Employee]
  implicit val ADecoder = deriveDecoder[Employee]
}
