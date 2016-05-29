package co.quine.osprey.twitter

import argonaut._, Argonaut._
import scalaj.http.{Http, HttpRequest, HttpResponse, Token => HttpToken}
import co.quine.gatekeeperclient._
import co.quine.gatekeeperclient.GatekeeperClient._

object Resources {

  implicit class HttpRequests(request: HttpRequest) {
    def sign(implicit gate: GatekeeperClient) = {
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
        case Unavailable(ttl) =>
          HttpResponse[String](s"""{"rate_limit": $ttl}""", 430, Map("TTL" -> IndexedSeq(ttl.toString)))
      }

      if (response.is2xx) {
        val remaining = response.header("x-rate-limit-remaining")
        val reset = response.header("x-rate-limit-reset")

        if (remaining.nonEmpty && reset.nonEmpty) {
          gate.updateRateLimit(token.key, resource, remaining.get.toInt, reset.get.toLong)
        }
      }
      response
    }
  }

  sealed trait TwitterResource {
    val uri = "https://api.twitter.com/1.1"
    val path: String

    def params: Map[String, String]

    def url = s"$uri$path"

    def get(implicit gate: GatekeeperClient) = Http(url).sign(gate)

    def token(implicit gate: GatekeeperClient): Token
  }

  case class UsersShow(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterResource {
    val path = "/users/show.json"

    def params = Seq(
      id map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _)).flatten.toMap

    def token(implicit gate: GatekeeperClient) = gate.usersShow
  }

  case class UsersLookup(id: Seq[Long] = Seq.empty[Long],
                         screenName: Seq[String] = Seq.empty[String]) extends TwitterResource {
    val path = "/users/lookup.json"

    def params = Seq(
      Some("user_id" -> id.mkString(",")),
      Some("screen_name" -> screenName.mkString(","))).flatten.toMap

    def token(implicit gate: GatekeeperClient) = gate.usersLookup
  }

  case class StatusesLookup(id: Seq[Long]) extends TwitterResource {
    val path = "/statuses/lookup.json"

    def params = Seq("id" -> id.mkString(",")).toMap

    def token(implicit gate: GatekeeperClient) = gate.statusesLookup
  }

  case class StatusesShow(id: Long) extends TwitterResource {
    val path = "/statuses/show.json"

    def params = Seq("id" -> id.toString).toMap

    def token(implicit gate: GatekeeperClient) = gate.statusesShow
  }

  case class StatusesUserTimeline(id: Option[Long] = None,
                                  screenName: Option[String] = None,
                                  sinceId: Option[Long] = None,
                                  maxId: Option[Long] = None,
                                  count: Int = 200) extends TwitterResource {
    val path = "/statuses/user_timeline.json"

    def params = Seq(
      id map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _.toString),
      sinceId map ("since_id" -> _.toString),
      maxId map ("max_id" -> _.toString),
      Some("count" -> count.toString)).flatten.toMap

    def token(implicit gate: GatekeeperClient) = gate.statusesUserTimeline
  }

  case class FriendsIds(id: Option[Long] = None,
                        screenName: Option[String] = None,
                        cursor: Option[Long] = None,
                        all: Boolean = false) extends TwitterResource {
    val path = "/friends/ids.json"

    def params = Seq(
      id map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _),
      cursor map ("cursor" -> _.toString)).flatten.toMap

    def token(implicit gate: GatekeeperClient) = gate.friendsIds
  }

  case class FriendsList(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterResource {
    val path = "/friends/list.json"

    def params = Seq(
      id map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _.toString)).flatten.toMap

    def token(implicit gate: GatekeeperClient) = gate.friendsList
  }

  case class FollowersIds(id: Option[Long] = None,
                          screenName: Option[String] = None,
                          cursor: Option[Long] = None,
                          all: Boolean = false) extends TwitterResource {
    val path = "/followers/ids.json"

    def params = Seq(
      id map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _),
      cursor map ("cursor" -> _.toString)).flatten.toMap

    def token(implicit gate: GatekeeperClient) = gate.followersIds
  }

  case class FollowersList(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterResource {
    val path = "/followers/list.json"

    def params = Seq(
      id map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _.toString)).flatten.toMap

    def token(implicit gate: GatekeeperClient) = gate.followersList
  }

}