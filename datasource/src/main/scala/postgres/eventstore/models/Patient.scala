package com.eztier.postgres.eventstore.models

case class Patient(name: String) extends Model {
  def parse(): List[String] = List(name)
}
