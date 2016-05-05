package co.quine.osprey.twitter

import akka.actor._
import akka.util.Timeout

import scalaj.http._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import co.quine.osprey._
import co.quine.osprey.twitter.api._

trait TwitterService
  extends Followers
    with Friends
    with Statuses
    with Users {
  self: Osprey =>

  implicit val system: ActorSystem
  implicit lazy val timeout = Timeout(5.seconds)

  val uri = "https://api.twitter.com/1.1"

  lazy val consumer = Await.result(gatekeeper.consumerToken, 30.seconds) match {
    case gatekeeper.ResponseToken(id, token: gatekeeper.ConsumerToken) => Token(token.key, token.secret)
  }

  def get(path: String, params: Map[String, String]): HttpResponse[String] = {
    var gateRequestId: String = ""
    val request = Http(path).params(params)
    val signed = getCredential(path) match {
      case gatekeeper.ResponseToken(requestId, token: gatekeeper.GateToken) =>
        gateRequestId = requestId
        token match {
          case gatekeeper.AccessToken(k, s) => request.oauth(consumer, Token(k, s))
          case gatekeeper.BearerToken(k) => request.header("Authorization", s"Bearer $k")
        }
    }

    val response = signed.asString
    /***
    if (response.code == 200) {
      val remaining: Int = response.header("X-Rate-Limit-Remaining").getOrElse(0).asInstanceOf[Int]
      val reset = response.header("X-Rate-Limit-Reset").getOrElse(0).asInstanceOf[Long]
      gatekeeper.updateRateLimit(gateRequestId, remaining, reset)
    }
      ***/
    response
  }

  def getCredential(path: String): gatekeeper.GateResponse = {
    val gateResponse = path.diff(uri) match {
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
    Await.result(gateResponse, 30.seconds)
  }
}