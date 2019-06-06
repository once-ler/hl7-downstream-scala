package package com.eztier.datasource.common.traits

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.CsvFormatting.{Backslash, Comma, DoubleQuote}
import akka.stream.alpakka.csv.scaladsl.{CsvFormatting, CsvParsing, CsvQuotingStyle, CsvToMap}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.collection.immutable

trait WithCsvSuppport {
  val csvFormat: Flow[immutable.Seq[String], ByteString, _]
  = CsvFormatting.format(
    Comma,
    DoubleQuote,
    Backslash,
    "\r\n",
    CsvQuotingStyle.Required,
    charset = StandardCharsets.UTF_8,
    byteOrderMark = None)

  def exportAsCsv(fileName: String, l: List[List[String]])(implicit rc: ExecutionContext, materializer: Materializer) = {
    val workingDir = System.getProperty("user.dir")
    val file = Paths.get(s"$workingDir/__internal__/$fileName")

    // Future bytestring
    val f = Source(l)
      .via(CsvFormatting.format())
      .toMat(FileIO.toPath(file))(Keep.right)
      .run()

    val f1 = for {
      z <- f
    } yield z

    Await.result(f1, Duration.Inf)
  }

  def formatAsCsv(l: List[List[String]])(implicit rc: ExecutionContext, materializer: Materializer) = {
    val f = Source(l)
      .via(CsvFormatting.format())
      .map(_.utf8String)
      .runWith(Sink.seq)

    Await.result(f, Duration.Inf).mkString("")
  }

  def importAsText(filePathAndName: String)(implicit rc: ExecutionContext, materializer: Materializer): Source[String, NotUsed] = {
    val workingDir = System.getProperty("user.dir")
    val file = Paths.get(s"$workingDir/$filePathAndName")

    FileIO.fromPath(file)
      .via(Framing.delimiter(
        ByteString("\n"),
        maximumFrameLength = 1024))
      .map(_.utf8String)
  }
}
