package co.quine.osprey.actors

import akka.actor._
import akka.io.{IO, Tcp}
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable
import co.quine.osprey.config._

object ServerActor {
  case class ClientDisconnected()

  val tempNumber = new AtomicLong(1)

  def tempName = tempNumber.getAndIncrement

  def props(osprey: ActorRef): Props = Props(new ServerActor(osprey))
}

class ServerActor(osprey: ActorRef) extends Actor with ActorLogging {

  val host = Server.host
  val port = Server.port

  val connectedClients = mutable.Set[ActorRef]()

  IO(Tcp)(context.system) ! Tcp.Bind(self, new InetSocketAddress(host, port))

  override def preStart() = log.info(s"${self.path.name}: up")

  def receive = {
    case Tcp.Connected(remote, local) =>
      startClient(sender, remote)
    case Tcp.Bound(localAddress) =>
      log.info(s"Server bound to $localAddress")
    case Tcp.CommandFailed(cmd) =>
      log.info(s"Command failed: $cmd")
    case Terminated(actor) => removeClient(actor)
  }

  def startClient(sender: ActorRef, remoteAddress: InetSocketAddress): ActorRef = {
    val clientId = s"client-${remoteAddress.getHostString}-" + ServerActor.tempName
    val clientActor = context.actorOf(ClientActor.props(osprey, sender), clientId)
    context.watch(clientActor)
    sender ! Tcp.Register(clientActor)
    connectedClients.add(clientActor)
    log.info(s"Client ($clientId): connected")
    clientActor
  }

  def removeClient(client: ActorRef) = {
    connectedClients.remove(client)
    log.info(s"Client (${client.path.name}) disconnected")
  }

}