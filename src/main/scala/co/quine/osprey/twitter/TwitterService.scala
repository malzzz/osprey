package co.quine.osprey.twitter

import akka.actor._
import akka.util.Timeout

import scalaj.http._
import scalaj.http.{Token => httpToken}
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import co.quine.gatekeeperclient._
import co.quine.osprey._
import co.quine.osprey.twitter.api._

trait TwitterService
  extends Followers
    with Friends
    with Statuses
    with Users {
  self: Osprey =>

  import GatekeeperClient._

  implicit val system: ActorSystem
  implicit val timeout = Timeout(5.seconds)

  val uri = "https://api.twitter.com/1.1"

  var consumer: httpToken = _

  gatekeeper.consumerToken onSuccess {
    case ConsumerToken(k, s) => consumer = httpToken(k, s)
  }

  implicit class HttpRequests(request: HttpRequest) {
    def signThenGetResponse: Future[HttpResponse[String]] = {
      val futureResponse = Promise[HttpResponse[String]]()
      val path = request.url.diff(uri)
      val token = path match {
        case "/users/show.json" => gatekeeper.usersShow
        case "/users/lookup.json" => gatekeeper.usersLookup
        case "/statuses/lookup.json" => gatekeeper.statusesLookup
        case "/statuses/show.json" => gatekeeper.statusesShow
        case "/statuses/user_timeline.json" => gatekeeper.statusesUserTimeline
        case "/friends/ids.json" => gatekeeper.friendsIds
        case "/friends/list.json" => gatekeeper.friendsList
        case "/followers/ids.json" => gatekeeper.followersIds
        case "/followers/list.json" => gatekeeper.followersList
      }

      token onSuccess {
        case AccessToken(k, s) => futureResponse.success(gateUpdate(path, k, request.oauth(consumer, Token(k, s))))
        case BearerToken(k) => futureResponse.success(gateUpdate(path, k, request.header("Authorization", s"Bearer $k")))
      }
      futureResponse.future
    }
  }

  def get(path: String, params: Map[String, String]): Future[HttpResponse[String]] = {
    Http(path).params(params).signThenGetResponse
  }

  def gateUpdate(path: String, key: String, request: HttpRequest) = {
    val response = request.asString
    (response.header("X-Rate-Limit-Remaining"), response.header("X-Rate-Limit-Reset")) match {
      case (Some(remaining), Some(reset)) => gatekeeper.updateRateLimit(key, path, remaining.toInt, reset.toLong)
      case _ =>
    }
    response
  }
}