package com.eztier.datasource.cassandra.dwh.implicits

import com.eztier.adapter.Hl7CassandraAdapter
import com.eztier.common.Configuration._
import com.eztier.hl7mock.types.{CaHl7, CaHl7Control, CaPatient, CaPatientControl}

object Transactors {
  val keySpace = conf.getString(s"$env.cassandra.keyspace")

  // TODO: Need to add overloaded constructor to accept Config object and not just configPath string.
  // This is needed production option for reading file from external file.
  implicit lazy val xa = Hl7CassandraAdapter[CaHl7, CaHl7Control]("$env.cassandra", keySpace)
  implicit lazy val xa1 = Hl7CassandraAdapter[CaPatient, CaPatientControl]("$env.cassandra", keySpace)
}
