package co.quine.osprey
package actors

import java.util.concurrent.atomic.AtomicLong

import akka.actor._

object OspreyActor {

  val tempNumber = new AtomicLong(1)

  def tempName = tempNumber.getAndIncrement

  def props() = Props(new OspreyActor())
}

class OspreyActor extends Actor with ActorLogging {

  import OspreyActor._
  import services._

  val requests = context.actorOf(HttpRequestActor.props, s"requests-$tempName")
  val twitterService = context.actorOf(TwitterActor.props(requests), s"twitter-service-$tempName")

  def receive = {
    case op: TwitterOperation => twitterService ! op
  }
}