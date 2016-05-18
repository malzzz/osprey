package co.quine.osprey.twitter
package api

import argonaut._, Argonaut._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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

  def userTimeline(userId: Option[Long] = None, screenName: Option[String] = None, maxLength: Int = 3200) = {

    require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")

    val callTimeline: (Option[Long], Int) => Future[Seq[Tweet]] =
      statusesUserTimeline(userId, screenName, None, _: Option[Long], _: Int)

    def recurseTimeline(t: Seq[Tweet]): Future[Seq[Tweet]] = {
      val statusesCount = {
        val userStatuses = t.head.user.map(user => user.statuses_count).getOrElse(0)
        if (userStatuses > maxLength) maxLength else userStatuses
      }

      val countDifference = {
        val absDiff = statusesCount - t.length
        if (absDiff < 200) absDiff else 200
      }

      if (t.length < statusesCount && t.length < maxLength && statusesCount > 0) {
        val maxId = t.map(tweets => tweets.id).min - 1

        for {
          nextTimeline <- callTimeline(Some(maxId), countDifference)
          completeTimeline <- recurseTimeline(t ++ nextTimeline)
        } yield completeTimeline
      } else Future.successful(t)
    }
    callTimeline(None, 200).flatMap(recurseTimeline)
  }
}