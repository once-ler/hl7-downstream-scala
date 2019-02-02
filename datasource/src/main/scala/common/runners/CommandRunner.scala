package com.eztier.datasource.common.runners

import java.io.{PrintWriter, StringWriter}
import akka.stream.scaladsl.Source
import akka.event.LoggingAdapter
import scala.reflect.runtime.universe._

import cats.effect.IO

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

}
