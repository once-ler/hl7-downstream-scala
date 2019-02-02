package com.eztier.datasource.common.models

case class Patient(name: String) extends Model {
  def parse(): List[String] = List(name)
}
