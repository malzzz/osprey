package co.quine.osprey

import akka.actor._

import co.quine.osprey.actors._
import co.quine.osprey.twitter.TwitterService

class Osprey extends TwitterService {

  implicit val system = ActorSystem("osprey")
  implicit val ec = system.dispatcher

  val requests = system.actorOf(HttpRequestActor.props, "http-requests")


}