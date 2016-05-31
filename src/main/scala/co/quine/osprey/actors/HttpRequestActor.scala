package co.quine.osprey
package actors

import akka.actor._
import scalaj.http._
import scalaj.http.{Token => HttpToken}
import co.quine.gatekeeperclient._

object HttpRequestActor {
  def props = Props(new HttpRequestActor())
}

class HttpRequestActor extends Actor with ActorLogging {

  import resources._
  import responses._
  import GatekeeperClient._

  implicit val gatekeeper = GatekeeperClient(Some(context.system))

  lazy val consumer: HttpToken = {
    val token = gatekeeper.consumerToken
    HttpToken(token.key, token.secret)
  }

  override def preStart() = {
    log.info(s"${self.path.name}: Ready")
  }

  def receive = {
    case r: TwitterResource => sender ! HttpStringResponse(get(r))
  }

  def get(resource: TwitterResource): HttpResponse[String] = {
    val request = Http(resource.url).params(resource.params)
    request.sign
  }
}