package com.eztier.postgres.eventstore.models

import java.util.Date

trait IControl {
  def model: String,
  def subscriber: String,
  def startTime: Date
}

trait MasterControl

sealed case class VersionControl(model: String, subscriber: String, startTime: Date) extends MasterControl with IControl
sealed case class CaPatientControl(model: String, subscriber: String, startTime: Date) extends IControl
