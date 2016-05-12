package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._
import scala.concurrent.ExecutionContext

trait Statuses {
  self: TwitterService =>

  import Codec._

  def statusesLookup(id: Seq[String])(implicit ec: ExecutionContext) = {

    val params = Seq("id" -> id.mkString(",")).toMap

    get(s"$uri/statuses/lookup.json", params) map { r =>
      Parse.decodeOr[Seq[Tweet], Seq[Tweet]](r.body, _.seq, Seq.empty[Tweet])
    }
  }

  def statusesShow(id: String)(implicit ec: ExecutionContext) = {

    val params = Seq("id" -> id).toMap

    get(s"$uri/statuses/show.json", params).map(r => Parse.decodeOption[Tweet](r.body))
  }

  def statusesUserTimeline(userId: Option[Long] = None,
                           screenName: Option[String] = None,
                           sinceId: Option[Long] = None,
                           maxId: Option[Long] = None,
                           count: Int = 200)(implicit ec: ExecutionContext) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val params = Seq(
      userId map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _.toString),
      sinceId map ("since_id" -> _.toString),
      maxId map ("max_id" -> _.toString),
      Some("count" -> count.toString)).flatten.toMap

    get(s"$uri/statuses/user_timeline.json", params) map { r =>
      Parse.decodeOr[Seq[Tweet], Seq[Tweet]](r.body, _.seq, Seq.empty[Tweet])
    }
  }

  /***
  def completeTimeline(userId: Option[Long] = None, screenName: Option[String] = None) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    case class Timeline()

    val maxTweets = 3200

    def recurseTimeline(timeline: Seq[Tweet]): Seq[Tweet] = {
      if (timeline.length < maxTweets && timeline.length < timeline.head.user.map(u => u.statuses_count).getOrElse(0)) {
        val maxId = timeline.min(Ordering.by((t: Tweet) => t.id)).id - 1
        val nextTimeline = statusesUserTimeline(userId = userId, screenName = screenName, maxId = Some(maxId))
        val newTimeline = timeline ++ nextTimeline
        recurseTimeline(newTimeline)
      } else timeline.distinct
    }
    recurseTimeline(statusesUserTimeline(userId, screenName))
  }
    **/
}