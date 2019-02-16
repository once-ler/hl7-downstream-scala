package com.eztier.datasource.cassandra.dwh.implicits

import akka.stream.scaladsl.Source
import com.datastax.driver.core.{ResultSet, SimpleStatement}
import com.eztier.adapter.Hl7CassandraAdapter
import com.eztier.hl7mock.types.{CaHl7, CaHl7Control, CaPatient, CaPatientControl}
import com.eztier.datasource.cassandra.dwh.implicits.Transactors.{xaCaHl7, xaCaPatient}
import com.eztier.hl7mock.{CaBase, CaControl}

import scala.concurrent.Future

trait Updatable[A <: CaBase, B <: CaControl] {
  def update(msg: String): Future[Int]
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

object Searchable {
  implicit object Hl7MessageSearch extends Searchable[CaHl7, CaHl7Control] {
    override def search(stmt: String): Future[ResultSet] = {
      val stmt1 = new SimpleStatement(stmt).setFetchSize(20)
      xaCaHl7.flow.provider.readAsync(stmt1)
    }
  }
}
