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

  import ClientActor._
  import ParserActor._
  import responses._
  import context.dispatcher

  implicit val timeout = Timeout(5.seconds)

  val parser: ActorRef = context.actorOf(ParserActor.props(self))

  val testRequest =
    """
      |{
      |   "uuid": "abc-1234-defg-5678",
      |   "service": "twitter",
      |   "method": "susertimeline",
      |   "args": { "screen_name": "charli_xcx", "count": 200 },
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
    case r: ServiceResponse => onResponse(r)
  }

  def onResponse(r: ServiceResponse) = {
    val encodedResponse = s"${r.uuid}##${r.response.nospaces}"
    val responseString = s"$$${encodedResponse.length}\r\n$encodedResponse\r\n"
    writeToSocket(responseString)
  }

  def onRequest(r: ClientRequest) = {
    val op = TwitterOperation(r.uuid, r.resource, self)
    osprey ! op
  }

  def writeToSocket(s: String) = {
    client ! Write(ByteString(s), WriteAck)
  }

}