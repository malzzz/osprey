package co.quine.osprey

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.Tcp._

import scala.concurrent._

import co.quine.gatekeeperclient._
import co.quine.osprey.twitter.TwitterService

class Osprey extends TwitterService {

  implicit val system = ActorSystem("osprey")
  implicit val materializer = ActorMaterializer()

  implicit val gatekeeper = new GatekeeperClient()

}