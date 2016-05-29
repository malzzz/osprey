package co.quine.osprey
package actors

import akka.actor._
import scalaj.http._
import scalaj.http.{Token => HttpToken}
import co.quine.gatekeeperclient._
import co.quine.osprey.twitter.Resources._

object HttpRequestActor {
  def props = Props(new HttpRequestActor())
}

class HttpRequestActor extends Actor with ActorLogging {

  implicit val gatekeeper = GatekeeperClient(Some(context.system))

  lazy val consumer: HttpToken = {
    val token = gatekeeper.consumerToken
    HttpToken(token.key, token.secret)
  }

  override def preStart() = {
    log.info(s"${self.path.name}: Ready")
  }

  def receive = {
    case r: TwitterResource =>
      sender ! get(r)
  }

  def get(resource: TwitterResource): HttpResponse[String] = {
    val request = Http(resource.url).params(resource.params)
    request.sign
  }
}