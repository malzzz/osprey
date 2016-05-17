package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait Statuses {
  self: TwitterService =>

  import Codec._

  def statusesLookup(id: Seq[String]) = {

    val params = Seq("id" -> id.mkString(",")).toMap

    get(Resources.StatusesLookup(params)) map { r =>
      Parse.decodeOr[Seq[Tweet], Seq[Tweet]](r.body, _.seq, Seq.empty[Tweet])
    }
  }

  def statusesShow(id: String) = {

    val params = Seq("id" -> id).toMap

    get(Resources.StatusesShow(params)).map(r => Parse.decodeOption[Tweet](r.body))
  }

  def statusesUserTimeline(userId: Option[Long] = None,
                           screenName: Option[String] = None,
                           sinceId: Option[Long] = None,
                           maxId: Option[Long] = None,
                           count: Int = 200) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val params = Seq(
      userId map ("user_id" -> _.toString),
      screenName map ("screen_name" -> _.toString),
      sinceId map ("since_id" -> _.toString),
      maxId map ("max_id" -> _.toString),
      Some("count" -> count.toString)).flatten.toMap

    get(Resources.StatusesUserTimeline(params)) map { r =>
      Parse.decodeOr[Seq[Tweet], Seq[Tweet]](r.body, _.seq, Seq.empty[Tweet])
    }
  }

  def completeTimeline(userId: Option[Long] = None, screenName: Option[String] = None) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val maxTweets = 3200

    def getTimeline(userId: Option[Long] = None, screenName: Option[String] = None, maxId: Option[Long] = None) = {
      val timeline = statusesUserTimeline(userId = userId, screenName = screenName, maxId = maxId)
      Await.result(timeline, 5.seconds)
    }

    def recurseTimeline(timeline: Seq[Tweet]): Seq[Tweet] = {
      if (timeline.length < maxTweets && timeline.length < timeline.head.user.map(u => u.statuses_count).getOrElse(0)) {
        val maxId = timeline.min(Ordering.by((t: Tweet) => t.id)).id - 1
        val nextTimeline = getTimeline(userId = userId, screenName = screenName, maxId = Some(maxId))
        val newTimeline = timeline ++ nextTimeline
        recurseTimeline(newTimeline)
      } else timeline.distinct
    }
    recurseTimeline(getTimeline(userId, screenName))
  }
}