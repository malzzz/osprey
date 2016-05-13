package co.quine.osprey.twitter

import scalaj.http.{Http, HttpRequest, HttpResponse, Token => HttpToken}
import scala.concurrent.Future
import co.quine.gatekeeperclient._
import co.quine.gatekeeperclient.GatekeeperClient._
import co.quine.osprey.actors.HttpRequestActor._

object Resources {

  implicit class HttpRequests(request: HttpRequest) {
    def sign(implicit gate: GatekeeperClient) = {
      println(request.url)
      val resource = request.url.diff("https://api.twitter.com/1.1")
      val token = resource match {
        case "/users/show.json" => gate.usersShow
        case "/users/lookup.json" => gate.usersLookup
        case "/statuses/lookup.json" => gate.statusesLookup
        case "/statuses/show.json" => gate.statusesShow
        case "/statuses/user_timeline.json" => gate.statusesUserTimeline
        case "/friends/ids.json" => gate.friendsIds
        case "/friends/list.json" => gate.friendsList
        case "/followers/ids.json" => gate.followersIds
        case "/followers/list.json" => gate.followersList
      }

      val response: HttpResponse[String] = token match {
        case AccessToken(k, s) =>
          val consumer = gate.defaultConsumer
          request.oauth(HttpToken(consumer.key, consumer.secret), HttpToken(k, s)).asString
        case BearerToken(k) => request.header("Authorization", s"Bearer $k").asString
      }

      val remaining = response.header("x-rate-limit-remaining")
      val reset = response.header("x-rate-limit-reset")

      if (remaining.nonEmpty && reset.nonEmpty) {
        gate.updateRateLimit(token.key, resource, remaining.get.toInt, reset.get.toLong)
      }
      response
    }
  }

  sealed trait TwitterResource {
    val uri = "https://api.twitter.com/1.1"
    val path: String
    val params: Map[String, String]

    def url = s"$uri$path"

    def get(implicit gate: GatekeeperClient) = Http(url).sign(gate)

    def token(implicit gate: GatekeeperClient): Token
  }

  case class UsersShow(params: Map[String, String]) extends TwitterResource {
    val path = "/users/show.json"

    def token(implicit gate: GatekeeperClient) = gate.usersShow
  }

  case class UsersLookup(params: Map[String, String]) extends TwitterResource {
    val path = "/users/lookup.json"

    def token(implicit gate: GatekeeperClient) = gate.usersLookup
  }

  case class StatusesLookup(params: Map[String, String]) extends TwitterResource {
    val path = "/statuses/lookup.json"

    def token(implicit gate: GatekeeperClient) = gate.statusesLookup
  }

  case class StatusesShow(params: Map[String, String]) extends TwitterResource {
    val path = "/statuses/show.json"

    def token(implicit gate: GatekeeperClient) = gate.statusesShow
  }

  case class StatusesUserTimeline(params: Map[String, String]) extends TwitterResource {
    val path = "/statuses/user_timeline.json"

    def token(implicit gate: GatekeeperClient) = gate.statusesUserTimeline
  }

  case class FriendsIds(params: Map[String, String]) extends TwitterResource {
    val path = "/friends/ids.json"

    def token(implicit gate: GatekeeperClient) = gate.friendsIds
  }

  case class FriendsList(params: Map[String, String]) extends TwitterResource {
    val path = "/friends/list.json"

    def token(implicit gate: GatekeeperClient) = gate.friendsList
  }

  case class FollowersIds(params: Map[String, String]) extends TwitterResource {
    val path = "/followers/ids.json"

    def token(implicit gate: GatekeeperClient) = gate.followersIds
  }

  case class FollowersList(params: Map[String, String]) extends TwitterResource {
    val path = "/followers/list.json"

    def token(implicit gate: GatekeeperClient) = gate.followersList
  }

}