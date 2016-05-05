package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._

trait Users {
  self: TwitterService =>

  import Codec._

  def usersShow(userId: Option[String] = None, screenName: Option[String] = None) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val params = Seq(
      userId map ("user_id" -> _),
      screenName map ("screen_name" -> _)).flatten.toMap

    Parse.decodeOption[User](get(s"$uri/users/show.json", params).body)
  }

  def usersLookup(userId: Seq[String] = Seq.empty[String], screenName: Seq[String] = Seq.empty[String]) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Must specify either user_id or screen_name")

    val params = Seq(
      Some("user_id" -> userId.mkString(",")),
      Some("screen_name" -> screenName.mkString(","))
    ).flatten.toMap

    Parse.decodeOption[Seq[User]](get(s"$uri/users/lookup.json", params).body)
  }
}