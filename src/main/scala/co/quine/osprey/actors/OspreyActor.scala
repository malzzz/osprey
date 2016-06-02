package co.quine.osprey
package actors

import java.util.concurrent.atomic.AtomicLong

import akka.actor._

object OspreyActor {

  val metricsNumber = new AtomicLong(1)
  val requestsNumber = new AtomicLong(1)
  val twitterNumber = new AtomicLong(1)

  def metricsName = metricsNumber.getAndIncrement
  def requestsName = requestsNumber.getAndIncrement
  def twitterName = twitterNumber.getAndIncrement

  def props() = Props(new OspreyActor())
}

class OspreyActor extends Actor with ActorLogging {

  import OspreyActor._
  import metrics._
  import services._
  import responses._

  val metricsActor = context.actorOf(MetricsActor.props, s"metrics-$metricsName")
  val requestsActor = context.actorOf(HttpRequestActor.props, s"requests-$requestsName")
  val twitterActor = context.actorOf(TwitterActor.props(self, requestsActor), s"twitter-$twitterName")

  def receive = {
    case operation: Operation => onOperation(operation)
    case response: ServiceResponse => onResponse(response)
  }

  def onOperation(operation: Operation) = operation match {
    case op: TwitterOperation =>
      twitterActor ! op
      metricsActor ! OperationMetric(op.uuid, op.resource.path, System.currentTimeMillis)
  }

  def onResponse(response: ServiceResponse) = {
    response.client ! response

    response.response match {
      case r: TwitterResponse with TwitterMetricData =>
        metricsActor ! ResponseMetric(response.uuid, "twitter", r.recordCount, System.currentTimeMillis)
    }
  }

}