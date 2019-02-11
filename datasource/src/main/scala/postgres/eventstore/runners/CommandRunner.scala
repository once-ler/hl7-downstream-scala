package com.eztier.datasource.postgres.eventstore.runners

import akka.stream.scaladsl.Source
import java.util.UUID
import java.time.LocalDateTime

import scala.reflect.runtime.universe._
import com.eztier.datasource.postgres.eventstore.implicits._
import com.eztier.datasource.postgres.eventstore.implicits.Transactors._
import com.eztier.datasource.postgres.eventstore.models.{CaPatientControl, IEvent}

import com.eztier.datasource.common.runners.CommandRunner._

object CommandRunner {
  // These type classes depend on an implicit instance of Transactor

  def search[A](term: String, schema: String = "hl7")(implicit searchable: Searchable[A], typeTag: TypeTag[A]): Source[A, akka.NotUsed] = {
    val t = schema + "." + typeTag.tpe.typeSymbol.name.toString.toLowerCase

    val io = searchable.search(term)

    val src = tryRunIO(io)

    Source(src)
  }

  def searchLog[A](toStore: String, fromDateTime: LocalDateTime, toDateTime: LocalDateTime, schema: String = "ril")(implicit searchable: SearchableLog[A], typeTag: TypeTag[A]): Source[A, akka.NotUsed] = {
    val io = searchable.search(toStore, fromDateTime, toDateTime, schema)

    val src = tryRunIO(io)

    Source(src)
  }

  def adhoc[A](sqlstring: String, schema: String = "hl7")(implicit adhocable: AdHocable[A], typeTag: TypeTag[A]): Source[A, akka.NotUsed] = {
    val t = schema + "." + typeTag.tpe.typeSymbol.name.toString.toLowerCase

    val io = adhocable.adhoc(sqlstring)

    val src = tryRunIO(io)

    Source(src)
  }

  def update[A](a: A, schema: String = "hl7")(implicit updatable: Updatable[A], typeTag: TypeTag[A]) = {
    val io = updatable.update(a, schema)

    val src = tryRunIO(io)

    Source.single(src)
  }

  def create[A](primaryKeys: List[String], schema: String = "hl7")(implicit creatable: Creatable[A], typeTag: TypeTag[A]) = {
    val io = creatable.create(primaryKeys, schema)

    val src = tryRunIO(io)

    Source.single(src)
  }

  private def uuidV3(name: String, namespace: String = "ns:URL") = {
    val source = namespace + name
    val bytes = source.getBytes("UTF-8")
    val uuid = UUID.nameUUIDFromBytes(bytes)
    uuid.toString
  }

  def addEvent[A <: IEvent](events: List[A] = List(), schema: String = "hl7")(implicit eventable: Eventable[A]) = {
    val streamType = events.headOption match {
      case Some(a) => Some(a.eventType)
      case _ => None
    }

    streamType match {
      case Some(streamType) =>
        val streamUuid = uuidV3(streamType)
        val eventUuids = events.map(a => UUID.randomUUID().toString())
        val eventTypes = events.map(_.eventType)
        val bodies = events.map(_.data)

        val io = eventable.event(streamUuid, streamType, eventUuids, eventTypes, bodies, schema)

        val src = tryRunIO(io)
        Source(src)

      case _ => Source.empty
    }
  }
}
