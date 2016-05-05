package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext.Implicits.global

trait Statuses {
  self: TwitterService =>

  import Codec._

  def statusesLookup(id: Seq[String]) = {

    val params = Seq("id" -> id.mkString(",")).toMap

    get(s"$uri/statuses/lookup.json", params).map(r => Parse.decodeOption[Seq[Tweet]](r.body))
  }

  def statusesShow(id: String) = {

    val params = Seq("id" -> id).toMap

    get(s"$uri/statuses/show.json", params).map(r => Parse.decodeOption[Tweet](r.body))
  }

  def statusesUserTimeline(userId: Option[String] = None, screenName: Option[String] = None) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val params = Seq(
      userId map ("user_id" -> _),
      screenName map ("screen_name" -> _)).flatten.toMap

    get(s"$uri/statuses/user_timeline.json", params).map(r => Parse.decodeOption[Seq[Tweet]](r.body))
  }
}