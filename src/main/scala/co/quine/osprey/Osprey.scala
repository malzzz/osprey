package co.quine.osprey

import akka.actor._
import co.quine.osprey.twitter.TwitterService

trait Osprey extends TwitterService {

  val requests: ActorRef

}