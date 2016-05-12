package co.quine.osprey.twitter

import scala.concurrent.Future
import co.quine.gatekeeperclient._
import co.quine.gatekeeperclient.GatekeeperClient.GateReply
import co.quine.osprey.actors.HttpRequestActor._

object Resources {

  sealed trait TwitterResource {
    val uri = "https://api.twitter.com/1.1"
    val path: String
    val params: Map[String, String]

    val url = s"$uri$path"

    def token(implicit gate: GatekeeperClient): Future[GateReply]

    def get = GetRequest(resource = this)
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