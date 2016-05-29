package co.quine.osprey
package actors

import scala.concurrent.duration._

import akka.actor._
import akka.io.Tcp._
import akka.util.{ByteString, Timeout}

object ClientActor {

  case object WriteAck extends Event

  def props(osprey: ActorRef, client: ActorRef): Props = Props(new ClientActor(osprey, client))
}

class ClientActor(osprey: ActorRef, client: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher
  import ClientActor._
  import ParserActor._

  implicit val timeout = Timeout(5.seconds)

  val parser: ActorRef = context.actorOf(ParserActor.props(self))

  val testRequest =
    """
      |{
      |   "uuid": "abc-1234-defg-5678",
      |   "service": "twitter",
      |   "method": "ushow",
      |   "args": { "screen_name": "charli_xcx" },
      |   "eta": "Some date"
      |}
    """.stripMargin

  override def preStart() = {
    val bs = ByteString(s"$$${testRequest.length}\r\n$testRequest\r\n")
    context.system.scheduler.scheduleOnce(5.seconds, parser, bs)
  }

  def receive = tcp orElse service

  def tcp: Receive = {
    case Received(bs) => parser ! bs
    case r: ClientRequest => onRequest(r)
    case (Closed | ErrorClosed | PeerClosed) =>
      context.stop(self)
  }

  def service: Receive = {
    case r: Response => onResponse(r)
  }

  def onResponse(r: Response) = {
    val encodedResponse = s"${r.uuid}##${r.payload.nospaces}"
    val responseString = s"$$${encodedResponse.length}\r\n$encodedResponse\r\n"
    writeToSocket(responseString)
  }

  def onRequest(r: ClientRequest) = {
    val op = TwitterOperation(r.uuid, r.resource, Some(r.eta), self)
    osprey ! op
  }

  def writeToSocket(s: String) = {
    client ! Write(ByteString(s), WriteAck)
  }

}