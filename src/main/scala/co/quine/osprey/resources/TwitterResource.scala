package co.quine.osprey
package resources

import argonaut._, Argonaut._

import co.quine.gatekeeperclient.GatekeeperClient._
import co.quine.gatekeeperclient._

import codecs.TwitterCodec._
import responses._

sealed trait TwitterResource {
  val uri = "https://api.twitter.com/1.1"
  val path: String

  def params: Map[String, String]

  def url = s"$uri$path"

  def token(implicit gate: GatekeeperClient): Token
}

sealed trait CursoredResource extends TwitterResource {
  val id: Option[Long]
  val screenName: Option[String]
  val cursor: Option[Long]
  val count: Int

  def maxCalls: Int

  def withCursor(c: Long): CursoredResource
}

sealed trait CursoredIdList extends CursoredResource

case class FollowersIds(id: Option[Long] = None,
                        screenName: Option[String] = None,
                        cursor: Option[Long] = None,
                        count: Int = 5000) extends CursoredIdList {
  val path = "/followers/ids.json"

  def maxCalls = math.ceil(count.toDouble / 5000).toInt

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _),
    cursor map ("cursor" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.followersIds

  def withCursor(c: Long) = copy(cursor = Some(c))

  def decode(s: String): UserIds = Parse.decodeOption[UserIds](s).getOrElse(UserIds(0, Seq.empty[Long], 0))
}

case class FriendsIds(id: Option[Long] = None,
                      screenName: Option[String] = None,
                      cursor: Option[Long] = None,
                      count: Int = 5000) extends CursoredIdList {
  val path = "/friends/ids.json"

  def maxCalls = math.ceil(count.toDouble / 5000).toInt

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _),
    cursor map ("cursor" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.friendsIds

  def withCursor(c: Long) = copy(cursor = Some(c))

  def decode(s: String): UserIds = Parse.decodeOption[UserIds](s).getOrElse(UserIds(0, Seq.empty[Long], 0))
}

sealed trait CursoredList extends CursoredResource

case class FriendsList(id: Option[Long] = None,
                       screenName: Option[String] = None,
                       cursor: Option[Long] = None,
                       count: Int = 200) extends CursoredList {
  val path = "/friends/list.json"

  def maxCalls = math.ceil(count.toDouble / 200).toInt

  def withCursor(c: Long) = copy(cursor = Some(c))

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.friendsList

  def decode(s: String): UserList = Parse.decodeOption[UserList](s).getOrElse(UserList(0, 0, Seq.empty[User]))
}

case class FollowersList(id: Option[Long] = None,
                         screenName: Option[String] = None,
                         cursor: Option[Long] = None,
                         count: Int = 200) extends CursoredList {

  val path = "/followers/list.json"

  def maxCalls = math.ceil(count.toDouble / 200).toInt

  def withCursor(c: Long) = copy(cursor = Some(c))

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.followersList

  def decode(s: String): UserList = Parse.decodeOption[UserList](s).getOrElse(UserList(0, 0, Seq.empty[User]))
}

case class UsersShow(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterResource {
  val path = "/users/show.json"

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.usersShow

  def decode(s: String): Option[User] = Parse.decodeOption[User](s)
}

case class UsersLookup(id: Seq[Long] = Seq.empty[Long],
                       screenName: Seq[String] = Seq.empty[String]) extends TwitterResource {
  val path = "/users/lookup.json"

  def params = Seq(
    Some("user_id" -> id.mkString(",")),
    Some("screen_name" -> screenName.mkString(","))).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.usersLookup

  def decode(s: String): Seq[User] = Parse.decodeOption[Seq[User]](s).getOrElse(Seq.empty[User])
}

case class StatusesLookup(id: Seq[Long]) extends TwitterResource {
  val path = "/statuses/lookup.json"

  def params = Seq("id" -> id.mkString(",")).toMap

  def token(implicit gate: GatekeeperClient) = gate.statusesLookup

  def decode(s: String): Seq[Tweet] = Parse.decodeOption[Seq[Tweet]](s).getOrElse(Seq.empty[Tweet])
}

case class StatusesShow(id: Long) extends TwitterResource {
  val path = "/statuses/show.json"

  def params = Seq("id" -> id.toString).toMap

  def token(implicit gate: GatekeeperClient) = gate.statusesShow

  def decode(s: String): Option[Tweet] = Parse.decodeOption[Tweet](s)
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

  def withCount(count: Int) = copy(count = count)
  def withMaxId(maxId: Long) = copy(maxId = Some(maxId))
  def withMaxId(maxId: Long, count: Int) = copy(maxId = Some(maxId), count = count)

  def decode(s: String): Seq[Tweet] = Parse.decodeOption[Seq[Tweet]](s).getOrElse(Seq.empty[Tweet])
}


