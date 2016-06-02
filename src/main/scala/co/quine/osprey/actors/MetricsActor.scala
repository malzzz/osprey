package co.quine.osprey
package actors

import akka.actor._
import com.github.nscala_time.time.Imports._
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicLong

object MetricsActor {

  val cassandraNumber = new AtomicLong(1)

  def cassandraName = cassandraNumber.getAndIncrement

  def props = Props(new MetricsActor())
}

class MetricsActor extends Actor with ActorLogging {

  import CassandraActor._
  import MetricsActor._
  import metrics._

  val cassandraActor = context.actorOf(CassandraActor.props, s"cassandra-$cassandraName")
  val pendingOperations = mutable.ArrayBuffer[OperationMetric]()

  def receive = {
    case m: Metric => onMetric(m)
  }

  def onMetric(metric: Metric) = metric match {
    case m: OperationMetric => pendingOperations += m
    case m: ResponseMetric => onResponse(m)
  }

  def onResponse(metric: ResponseMetric) = {
    pendingOperations.find(_.uuid == metric.uuid) foreach { op =>
      val date = {
        val dtf = DateTimeFormat.forPattern("yyyy-MM-dd")
        DateTime.now.toString(dtf)
      }

      val opTime = DateTime.now
      val opDurationMs = metric.timeMillis - op.timeMillis

      metric.service match {
        case "twitter" =>
          cassandraActor ! TwitterOperationRecord(op.resource, date, opTime, opDurationMs.toInt, metric.records)
      }
    }
  }

}