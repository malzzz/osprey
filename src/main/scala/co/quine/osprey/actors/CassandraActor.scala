package co.quine.osprey
package actors

import akka.actor._
import com.datastax.driver.core._
import com.github.nscala_time.time.Imports._

object CassandraActor {
  sealed trait Record
  case class TwitterOperationRecord(resource: String,
                                    date: String,
                                    opTime: DateTime,
                                    opDurationMillis: Int,
                                    recordsReturned: Int) extends Record

  val keyspace =
    """
      |CREATE KEYSPACE IF NOT EXISTS osprey_metrics
      |WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '2'}
      |AND durable_writes = true
    """.stripMargin

  val twitterOperationsTable =
    """
      |CREATE TABLE IF NOT EXISTS osprey_metrics.twitter_operations (
      |resource varchar,
      |date varchar,
      |op_time timestamp,
      |op_duration_ms int,
      |records_returned int,
      |PRIMARY KEY ((resource, date), op_time)
      |);
    """.stripMargin

  val twitterOperationRecord =
    s"""
      |INSERT INTO osprey_metrics.twitter_operations (resource, date, op_time, op_duration_ms, records_returned)
      |VALUES (?, ?, ?, ?, ?);
    """.stripMargin

  def props = Props(new CassandraActor())
}

class CassandraActor extends Actor with ActorLogging {

  import CassandraActor._
  import config.Backends._

  var cluster: Cluster = _
  var clusterMetadata: Metadata = _
  var preparedTwitterOperationRecord: PreparedStatement = _

  implicit lazy val session = cluster.connect()

  override def preStart() = {
    cluster = buildCluster
    clusterMetadata = cluster.getMetadata
    log.info(s"Connected to Cassandra cluster: ${clusterMetadata.getClusterName}")
    verifyKeyspace
    verifySchema
    preparedTwitterOperationRecord = prepareStatement(twitterOperationRecord)
  }

  override def postStop() = close()

  def verifyKeyspace(implicit sess: Session) = {
    sess.executeAsync(keyspace)
  }

  def verifySchema(implicit sess: Session) = {
    sess.executeAsync(twitterOperationsTable)
  }

  def prepareStatement(statement: String)(implicit sess: Session) = sess.prepare(statement)

  def receive = {
    case r: TwitterOperationRecord => insertRecord(r)
  }

  def insertRecord(record: Record)(implicit sess: Session) = record match {
    case r: TwitterOperationRecord =>
      val boundStatement = new BoundStatement(preparedTwitterOperationRecord)
        .bind(
          r.resource: java.lang.String,
          r.date: java.lang.String,
          r.opTime.toDate: java.util.Date,
          r.opDurationMillis: java.lang.Integer,
          r.recordsReturned: java.lang.Integer)
      sess.executeAsync(boundStatement)
    case _ => log.info("Invalid record type")
  }

  def buildCluster = Cluster.builder()
    .addContactPoint(cassandraNode)
    .build()

  def close() = {
    session.close()
    cluster.close()
  }
}