package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._
import scalaz._
import scala.concurrent.ExecutionContext.Implicits.global

trait Users {
  self: TwitterService =>

  import Codec._
  import Resources._

  def usersShow(userId: Option[Long] = None, screenName: Option[String] = None) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val params = Seq(
      userId map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _)).flatten.toMap

    val futureResponse = get(Resources.UsersShow(params))

    onResponse(futureResponse).map(response => response.map(user => Parse.decodeOption[User](user)))

  }

  def usersLookup(userId: Seq[String] = Seq.empty[String], screenName: Seq[String] = Seq.empty[String]) = {

    require(userId.nonEmpty || screenName.nonEmpty, "Must specify either user_id or screen_name")

    val params = Seq(
      Some("user_id" -> userId.mkString(",")),
      Some("screen_name" -> screenName.mkString(","))
    ).flatten.toMap

    get(Resources.UsersLookup(params)).map(r => Parse.decodeOption[Seq[User]](r.body))
  }
}