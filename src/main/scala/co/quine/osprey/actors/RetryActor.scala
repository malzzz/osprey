package co.quine.osprey
package actors

import akka.actor._
import scala.collection.mutable
import scala.concurrent.duration._

object RetryActor {
  def props() = Props(new RetryActor())
}

class RetryActor extends Actor with ActorLogging {

  import context.dispatcher

  val opStore = mutable.ArrayBuffer[RetryOperation]()

  def receive = {
    case r: RetryOperation => onRetryOperation(r)
  }

  def onRetryOperation(r: RetryOperation) = {
    val timeDiff = r.ttl - (System.currentTimeMillis / 1000)
    opStore += r
    context.system.scheduler.scheduleOnce(timeDiff.seconds, r.serviceActor, r.increment)
  }
}