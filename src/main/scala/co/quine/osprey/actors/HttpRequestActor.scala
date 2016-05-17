package co.quine.osprey.actors

import akka.actor._
import scalaj.http._
import scalaj.http.{Token => HttpToken}
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import co.quine.gatekeeperclient._
import co.quine.osprey.twitter.Resources._

object HttpRequestActor {
  case class GetRequest(resource: TwitterResource, promise: Promise[HttpResponse[String]])

  def props = Props(new HttpRequestActor())
}

class HttpRequestActor extends Actor with ActorLogging {

  import HttpRequestActor._

  implicit val gatekeeper = GatekeeperClient(Some(context.system))

  lazy val consumer: HttpToken = {
    val token = gatekeeper.consumerToken
    HttpToken(token.key, token.secret)
  }

  def receive = {
    case GetRequest(resource, promise) =>
      val response = get(resource)
      promise.success(response)
  }

  def get(resource: TwitterResource): HttpResponse[String] = {
    val request = Http(resource.url).params(resource.params)
    request.sign
  }
}