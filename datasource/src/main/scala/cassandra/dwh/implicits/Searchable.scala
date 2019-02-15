package com.eztier.datasource.cassandra.dwh.implicits

import akka.stream.scaladsl.Source
import com.datastax.driver.core.{ResultSet, SimpleStatement}
import com.eztier.adapter.Hl7CassandraAdapter
import com.eztier.datasource.postgres.eventstore.models.CaPatientControl
import com.eztier.hl7mock.types.{CaHl7, CaHl7Control, CaPatient}

import scala.concurrent.Future

trait Updatable[A, B] {
  def update(msg: String)(implicit xa: Hl7CassandraAdapter[A, B]): Future[Int]
}

trait Searchable[A, B] {
  def search(stmt: String)(implicit xa: Hl7CassandraAdapter[A, B]): Future[ResultSet]
}

object Updatable {
  implicit object Hl7MessageUpdate extends Updatable[CaHl7, CaHl7Control] {
    override def update(msg: String)(implicit xa: Hl7CassandraAdapter[CaHl7, CaHl7Control]): Future[Int] = {
      val s = Source.single(msg)
      xa.flow.runWithRawStringSource(s)
    }
  }

  implicit object CaPatientUpdate extends Updatable[CaPatient, CaPatientControl] {
    override def update(msg: String)(implicit xa: Hl7CassandraAdapter[CaPatient, CaPatientControl]): Future[Int] = {
      val s = Source.single(msg)
      xa.flow.runWithRawStringSource(s)
    }
  }
}

object Searchable {
  implicit object Hl7MessageSearch extends Searchable[CaHl7, CaHl7Control] {
    override def search(stmt: String)(implicit xa: Hl7CassandraAdapter[CaHl7, CaHl7Control]): Future[ResultSet] = {
      val stmt1 = new SimpleStatement(stmt).setFetchSize(20)

      xa.flow.provider.readAsync(stmt1)
    }
  }
}
