package com.eztier.datasource.oracle.dwh.implicits

import cats.effect.IO
import doobie._
import doobie.implicits._
import java.time.LocalDateTime

import com.eztier.datasource.oracle.dwh.implicits.Transactors._
import com.eztier.datasource.oracle.dwh.models.Employee
import com.eztier.datasource.oracle.dwh.models.EmployeeImplicits._

trait Searchable[A] {
  def search(fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "HR")(implicit xa: Transactor[IO]): IO[List[A]]
}

trait AdHocable[A] {
  def adhoc(sqlstring: String)(implicit xa: Transactor[IO]): IO[List[A]]
}

object Searchable {
  implicit object EmployeeSearch extends Searchable[Employee] {
    override def search(fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "HR")(implicit xa: Transactor[IO]): IO[List[Employee]] = {
      val stmt = fr"""SELECT EMPLOYEE_ID, FIRST_NAME, LAST_NAME, JOB_ID, HIRE_DATE, SALARY
        from """ ++
        Fragment(schema, None) ++ fr".EMPLOYEES where HIRE_DATE >= " ++
        Fragment(s"to_date('${fromDateTime.toString().substring(0, 19)}', 'YYYY-MM-DD${"\"T\""}HH24:MI:SS')", None) ++ fr" and HIRE_DATE <= " ++
        Fragment(s"to_date('${toDateTime.toString().substring(0, 19)}', 'YYYY-MM-DD${"\"T\""}HH24:MI:SS')", None)

      stmt
        .query[Employee]
        .stream
        .compile
        .to[List]
        .transact(xa)
    }
  }

}
