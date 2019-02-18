package com.eztier.datasource.cassandra.dwh.implicits

import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

import akka.stream.scaladsl.Source
import com.datastax.driver.core.{ResultSet, SimpleStatement}
import com.eztier.hl7mock.types._
import com.eztier.datasource.cassandra.dwh.implicits.Transactors.{xaCaHl7, xaCaPatient, keySpace}
import com.eztier.hl7mock.{CaBase, CaControl}
// CaHl7Control insertAsync
import com.eztier.cassandra.CaCommon.camelToUnderscores
import com.eztier.hl7mock.CaCommonImplicits._

import scala.concurrent.Future

trait Updatable[A <: CaBase, B <: CaControl] {
  def update(msg: String): Future[Int]
}

trait DateUpdatable[A] {
  def update(dt: LocalDateTime): Future[ResultSet]
}

trait Searchable[A <: CaBase, B <: CaControl] {
  def search(stmt: String): Future[ResultSet]
}

object Updatable {

  implicit object Hl7MessageUpdate extends Updatable[CaHl7, CaHl7Control] {
    override def update(msg: String): Future[Int] = {
      val s = Source.single(msg)
      xaCaHl7.flow.runWithRawStringSource(s)
    }
  }

  implicit object CaPatientUpdate extends Updatable[CaPatient, CaPatientControl] {
    override def update(msg: String): Future[Int] = {
      val s = Source.single(msg)
      xaCaPatient.flow.runWithRawStringSource(s)
    }
  }

}

object DateUpdatable {
  implicit object CaHl7ControlDateUpdate extends DateUpdatable[CaHl7Control] {
    override def update(dt: LocalDateTime): Future[ResultSet] = {
      val x = camelToUnderscores(CaHl7Control().getClass.getSimpleName)
      val updateTime = Date.from(dt.atOffset(ZoneOffset.UTC).toInstant)
      val el = CaTableDateControl(Id = x, CreateDate = updateTime)
      val insertStatement = el.getInsertStatement(keySpace)
      val qs = insertStatement.getQueryString()

      xaCaPatient.flow.provider.insertAsync(insertStatement)
    }
  }
}

object Searchable {
  implicit object Hl7MessageSearch extends Searchable[CaHl7, CaHl7Control] {
    override def search(stmt: String): Future[ResultSet] = {
      val stmt1 = new SimpleStatement(stmt).setFetchSize(20)
      xaCaHl7.flow.provider.readAsync(stmt1)
    }
  }
}
