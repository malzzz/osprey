package co.quine.osprey
package resources

import scala.collection.mutable
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

  val resultStash = mutable.Set[Long]()

  def stashResult(r: TraversableOnce[Long]) = resultStash ++= r

  def maxCalls = math.ceil(count.toDouble / 5000).toInt

  def withCursor(c: Long): CursoredResource
}

case class FollowersIds(id: Option[Long] = None,
                        screenName: Option[String] = None,
                        cursor: Option[Long] = None,
                        count: Int = 5000) extends CursoredResource {
  val path = "/followers/ids.json"

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _),
    cursor map ("cursor" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.followersIds

  def withCursor(c: Long) = copy(cursor = Some(c))
}

case class FriendsIds(id: Option[Long] = None,
                      screenName: Option[String] = None,
                      cursor: Option[Long] = None,
                      count: Int = 5000) extends CursoredResource {
  val path = "/friends/ids.json"

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _),
    cursor map ("cursor" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.friendsIds

  def withCursor(c: Long) = copy(cursor = Some(c))
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

  def withCount(count: Int) = copy(count = count)
  def withMaxId(maxId: Long) = copy(maxId = Some(maxId))
  def withMaxId(maxId: Long, count: Int) = copy(maxId = Some(maxId), count = count)
}

case class FriendsList(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterResource {
  val path = "/friends/list.json"

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.friendsList
}

case class FollowersList(id: Option[Long] = None, screenName: Option[String] = None) extends TwitterResource {
  val path = "/followers/list.json"

  def params = Seq(
    id map ("user_id" -> _.toString),
    screenName map ("screen_name" -> _.toString)).flatten.toMap

  def token(implicit gate: GatekeeperClient) = gate.followersList
}

