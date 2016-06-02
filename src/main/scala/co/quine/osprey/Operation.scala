package co.quine.osprey

import akka.actor._
import resources._

sealed trait Operation
case class TwitterOperation(uuid: String, resource: TwitterResource, client: ActorRef) extends Operation

case class RetryOperation(operation: Operation, serviceActor: ActorRef, ttl: Long, retries: Int = 0) {
  def increment = copy(retries = retries + 1)
}

case class RetrySucceeded(uuid: String)

