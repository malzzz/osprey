package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext

trait Users {
  self: TwitterService =>

  import Codec._

  def usersShow(userId: Option[String] = None, screenName: Option[String] = None)(implicit ec: ExecutionContext) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val params = Seq(
      userId map ("user_id" -> _),
      screenName map ("screen_name" -> _)).flatten.toMap

    get(s"$uri/users/show.json", params).map(r => Parse.decodeOption[User](r.body))
  }

  def usersLookup(userId: Seq[String] = Seq.empty[String], screenName: Seq[String] = Seq.empty[String])(
    implicit ec: ExecutionContext) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Must specify either user_id or screen_name")

    val params = Seq(
      Some("user_id" -> userId.mkString(",")),
      Some("screen_name" -> screenName.mkString(","))
    ).flatten.toMap

    get(s"$uri/users/lookup.json", params).map(r => Parse.decodeOption[Seq[User]](r.body))
  }
}