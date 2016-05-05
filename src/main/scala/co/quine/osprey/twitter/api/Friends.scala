package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._
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

    Parse.decodeOption[UserList](get(s"$uri/friends/list.json", params).body)
  }

  def friendsIds(userId: Option[String] = None,
                 screenName: Option[String] = None) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Either a screen_name or a user_id must be provided")

    val params = Seq(
      userId map ("user_id" -> _),
      screenName map ("screen_name" -> _.toString)).flatten.toMap

    Parse.decodeOption[UserIds](get(s"$uri/friends/ids.json", params).body)
  }
}