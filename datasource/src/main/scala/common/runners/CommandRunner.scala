package com.eztier.datasource.common.runners

import java.io.{PrintWriter, StringWriter}
import akka.stream.scaladsl.Source
import akka.event.LoggingAdapter
import scala.reflect.runtime.universe._

import cats.effect.IO
import doobie._
import doobie.implicits._

import com.eztier.datasource.common.implicits.ExecutionContext._

object CommandRunner {
  
  def tryRunIO[A](io: IO[List[A]]) = {
    try {
      io.unsafeRunSync()
    } catch {
      case err: Exception =>
        val sw = new StringWriter
        err.printStackTrace(new PrintWriter(sw))
        implicitly[LoggingAdapter].error(sw.toString)   
      throw err
    }
  }

  def tryRunIO[A](io: IO[A]) = {
    try {
      io.unsafeRunSync()
    } catch {
      case err: Exception =>
        val sw = new StringWriter
        err.printStackTrace(new PrintWriter(sw))
        implicitly[LoggingAdapter].error(sw.toString)
        throw err
    }
  }

  
  def adhoc[A](q0: Query0[A])(implicit xa: Transactor[IO]) : Source[A, akka.NotUsed] = {
    val io = q0
      .stream
      .compile
      .to[List]
      .transact(xa)

    val src = tryRunIO(io)

    Source(src)
  }


}
