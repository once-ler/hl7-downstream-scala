package com.eztier.test

import java.text.SimpleDateFormat
import java.util
import java.util.Optional

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.{FunSpec, Matchers}
import akka.stream.alpakka.solr._
import akka.stream.alpakka.solr.scaladsl.{SolrFlow, SolrSink, SolrSource}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.eztier.datasource.cassandra.dwh.implicits.Transactors.xaCaPatient
import com.eztier.hl7mock.types.CaPatientControl

import scala.concurrent.Await

// import org.apache.solr.client.solrj.embedded.JettyConfig
import org.apache.solr.client.solrj.impl.{CloudSolrClient, ZkClientClusterStateProvider}
import org.apache.solr.client.solrj.io.stream.expr.{StreamExpressionParser, StreamFactory}
import org.apache.solr.client.solrj.io.stream.{CloudSolrStream, StreamContext, TupleStream}
import org.apache.solr.client.solrj.io.{SolrClientCache, Tuple}
import org.apache.solr.client.solrj.request.{CollectionAdminRequest, UpdateRequest}
// import org.apache.solr.cloud.{MiniSolrCloudCluster, ZkTestServer}
import org.apache.solr.common.SolrInputDocument

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class TestSolrSpec extends FunSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem()
  implicit val commitExecutionContext: ExecutionContext = ExecutionContext.global

  implicit val materializer: Materializer = ActorMaterializer()

  final val predefinedCollection = "patient"

  final val dobPattern = "yyyy-MM-dd"
  final val dobDateFormat = new SimpleDateFormat(dobPattern)

  //#init-client
  final val zookeeperPort = 9984
  final val zookeeperHost = s"127.0.0.1:$zookeeperPort/solr"
  implicit val solrClient: CloudSolrClient =
    new CloudSolrClient.Builder(util.Arrays.asList(zookeeperHost), Optional.empty()).build

  val caPatientControlToDoc: CaPatientControl => SolrInputDocument = { b =>
    val doc = new SolrInputDocument
    doc.setField("city", b.City)
    doc.setField("dateOfBirth", b.DateOfBirth)
    doc.setField("email", b.Email)
    doc.setField("ethnicity", b.Ethnicity)
    doc.setField("gender", b.Gender)
    doc.setField("mrn", b.Mrn)
    doc.setField("name", b.Name)
    doc.setField("phoneNumber", b.PhoneNumber)
    doc.setField("postalCode", b.PostalCode)
    doc.setField("race", b.Race)
    doc.setField("stateProvince", b.StateProvince)
    doc.setField("street", b.Street)
    doc
  }

  val tupleToCaPatientControl: Tuple => CaPatientControl = { t =>
    CaPatientControl(
      City = t.getString("city"),
      DateOfBirth = t.getDate("dateOfBirth"),
      Email = t.getString("email"),
      Ethnicity = t.getString("ethnicity"),
      Gender = t.getString("gender"),
      Mrn = t.getString("mrn"),
      Name = t.getString("name"),
      PhoneNumber = t.getString("phoneNumber"),
      PostalCode = t.getString("postalCode"),
      Race = t.getString("race"),
      StateProvince = t.getString("stateProvince"),
      Street = t.getString("street")
    )
  }

  // Stream should be from Cassandra Source[Tuple, NotUsed]
  val stream = getTupleStream(predefinedCollection)

  val casStream = xaCaPatient.flow.casFlow.getSourceStream("select * from dwh.ca_patient_control", 200)

  // Row to CaPatientControl
  import com.eztier.hl7mock.CaPatientImplicits._

  it("Should stream from cassandra to solr") {
    val f = casStream
      .map(a => rowToCaPatientControl(a))
      .map(a => WriteMessage.createUpsertMessage(a))
      .groupedWithin(5, 10.millis)
      .via(
        SolrFlow
          .typeds[CaPatientControl](
            predefinedCollection,
            SolrUpdateSettings(),
            binder = caPatientControlToDoc
          )
      )
      .runWith(Sink.ignore)
      // commit after stream ended
      .map { done =>
        solrClient.commit(predefinedCollection)
        done
      }(commitExecutionContext)

    val r = Await.result(f, Duration.Inf)

  }

  private def getTupleStream(collection: String): TupleStream = {
    //#tuple-stream
    val factory = new StreamFactory().withCollectionZkHost(collection, zookeeperHost)
    val solrClientCache = new SolrClientCache()
    val streamContext = new StreamContext()
    streamContext.setSolrClientCache(solrClientCache)

    val expression =
      StreamExpressionParser.parse(s"""search($collection, q=*:*, fl="mrn,name", sort="name asc")""")
    val stream: TupleStream = new CloudSolrStream(expression, factory)
    stream.setStreamContext(streamContext)

    val source = SolrSource
      .fromTupleStream(stream)
    //#tuple-stream
    source should not be null
    stream
  }
}
