package com.eztier.datasource.redis.session.implicits

import akka.util.Timeout
import com.redis.RedisClient
import scala.concurrent.duration._

import com.eztier.common.Configuration.{conf, env}
import com.eztier.datasource.common.implicits.ExecutionContext._

object Transactors {
  val server = conf.getString(s"$env.redis.server")
  val port = conf.getInt(s"$env.redis.port")

  implicit val timeout = Timeout(5 seconds)

  implicit lazy val xaRedisClient = RedisClient(server, port)
}