package co.quine.osprey

import akka.actor._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import co.quine.osprey.actors._

object Main extends App {

  implicit val system = ActorSystem("osprey")

  val osprey = system.actorOf(OspreyActor.props(), "osprey-main")
  val server = system.actorOf(ServerActor.props(osprey), "osprey-server")

  Await.result(system.whenTerminated, Duration.Inf)

}