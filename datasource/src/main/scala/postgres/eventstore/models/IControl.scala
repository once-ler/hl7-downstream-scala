package com.eztier.datasource.postgres.eventstore.models

import java.time.LocalDateTime

trait IControl {
  def model: String
  def subscriber: String
  def startTime: LocalDateTime
}

trait MasterControl

sealed case class VersionControl(model: String, subscriber: String, startTime: LocalDateTime) extends MasterControl with IControl
sealed case class CaPatientControl(model: String, subscriber: String, startTime: LocalDateTime) extends IControl
