package co.quine.osprey.twitter
package api

import argonaut._
import scala.concurrent.ExecutionContext.Implicits.global

trait Friends {
  self: TwitterService =>

  import Codec._

  def friendsList(userId: Option[String] = None,
                  screenName: Option[String] = None) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Either a screen_name or a user_id must be provided")

    val params = Seq(
      userId map ("user_id" -> _),
      screenName map ("screen_name" -> _.toString)).flatten.toMap

    get(Resources.FriendsList(params)).map(r => Parse.decodeOption[UserList](r.body))
  }

  def friendsIds(userId: Option[Long] = None,
                 screenName: Option[String] = None,
                 all: Boolean = false) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Either a screen_name or a user_id must be provided")

    val params = Seq(
      userId map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _)).flatten.toMap

    val resource = Resources.FriendsIds(params)

    usersIds(resource, all)
  }
}