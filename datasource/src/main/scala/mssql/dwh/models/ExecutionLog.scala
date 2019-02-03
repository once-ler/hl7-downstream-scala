package com.eztier.datasource.mssql.dwh.models

import java.util.Date

case class ExecutionLog(
  StartTime: Date,
  FromStore: String,
  ToStore: String,
  StudyId: String,
  WSI: String,
  Caller: String,
  Request: String,
  Response: String,
  Error: Byte
)
