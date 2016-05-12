package co.quine.osprey.twitter
package api

import argonaut._

import scala.concurrent.ExecutionContext

trait Followers {
  self: TwitterService =>

  import Codec._

  def followersList(userId: Option[String] = None,
                  screenName: Option[String] = None)(implicit ec: ExecutionContext) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Either a screen_name or a user_id must be provided")

    val params = Seq(
      userId map ("user_id" -> _),
      screenName map ("screen_name" -> _.toString)).flatten.toMap

    get(s"$uri/followers/list.json", params).map(r => Parse.decodeOption[UserList](r.body))
  }

  def followersIds(userId: Option[String] = None,
                 screenName: Option[String] = None)(implicit ec: ExecutionContext) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Either a screen_name or a user_id must be provided")

    val params = Seq(
      userId map ("user_id" -> _),
      screenName map ("screen_name" -> _.toString)).flatten.toMap

    get(s"$uri/followers/ids.json", params).map(r => Parse.decodeOption[UserIds](r.body))
  }
}