package co.quine.osprey.actors

import akka.actor._
import scalaj.http._
import scalaj.http.{Token => HttpToken}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import co.quine.gatekeeperclient._
import co.quine.osprey.twitter.Resources._

object HttpRequestActor {
  case class GetRequest(resource: TwitterResource)

  def props = Props(new HttpRequestActor())
}

class HttpRequestActor extends Actor with ActorLogging {

  import GatekeeperClient._
  import HttpRequestActor._

  implicit val system = context.system
  implicit val gatekeeper = new GatekeeperClient()

  var consumer: HttpToken = _

  override def preStart() = {
    gatekeeper.consumerToken andThen {
      case Success(token) => consumer = HttpToken(token.key, token.secret)
      case Failure(_) => throw new Exception("Could not get consumer token")
    }
  }

  def receive = {
    case GetRequest(resource) => get(resource).foreach(response => sender() ! response)
  }

  def get(resource: TwitterResource): Future[HttpResponse[String]] = {
    val request = Http(resource.url).params(resource.params)

    resource.token map {
      case AccessToken(k, s) => gateUpdateFromResponse(resource, k, request.oauth(consumer, HttpToken(k, s)).asString)
      case BearerToken(k) => gateUpdateFromResponse(resource, k, request.header("Authorization", s"Bearer $k").asString)
    }
  }

  def gateUpdateFromResponse(resource: TwitterResource, key: String, response: HttpResponse[String]) = {
    for {
      remaining <- response.header("X-Rate-Limit-Remaining")
      reset <- response.header("X-Rate-Limit-Reset")
    } yield gatekeeper.updateRateLimit(key, resource.path, remaining.toInt, reset.toLong)
    response
  }
}