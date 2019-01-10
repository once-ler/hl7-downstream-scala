package com.eztier.postgres.eventstore.models

trait IEvent {
  def eventType: String
  def data: String // Expect json string
}

case class GenericEvent(eventType: String, data: String) extends IEvent

case class AppendEventResult(result: List[Option[Int]])
