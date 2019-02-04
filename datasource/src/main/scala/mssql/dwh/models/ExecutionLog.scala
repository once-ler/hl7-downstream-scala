package com.eztier.datasource.mssql.dwh.models

import java.util.Date
import java.sql.Timestamp
// import java.lang.Short

case class ExecutionLog(
  StartTime: Option[Timestamp],
  FromStore: Option[String],
  ToStore: Option[String],
  StudyId: Option[String],
  WSI: Option[String],
  Caller: Option[String],
  Request: Option[String],
  Response: Option[String],
  Error: Option[Byte]
)
