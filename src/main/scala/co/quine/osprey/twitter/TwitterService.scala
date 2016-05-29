package co.quine.osprey.twitter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http._
import scalaz._

import argonaut._

import co.quine.gatekeeperclient.GatekeeperClient.Unavailable

trait TwitterService extends Api {

  import Codec._
  import Resources._

  def get(resource: TwitterResource): Future[HttpResponse[String]]

  def onResponse(futureResponse: Future[HttpResponse[String]]): Future[Unavailable \/ String] = {
    val response: Future[Unavailable \/ String] = futureResponse map {
      case r: HttpResponse[String] if r.code == 200 => \/-(r.body)
      case r: HttpResponse[String] if r.code == 430 =>
        -\/(Parse.decodeOption[Unavailable](r.body).getOrElse(Unavailable(System.currentTimeMillis / 1000)))
    }
    response
  }

  def usersIds(resource: TwitterResource, all: Boolean = false): Future[UserIds] = {

    require(resource.isInstanceOf[FriendsIds] || resource.isInstanceOf[FollowersIds], "Invalid resource provided")

    def callIds: Future[UserIds] = get(resource) map { r =>
      Parse.decodeOption[UserIds](r.body).getOrElse(UserIds(0, Seq.empty[Long], 0))
    }

    def resourceWithCursor(cursor: Long) = resource match {
      case r: FriendsIds => r.copy(cursor = Some(cursor))
      case r: FollowersIds => r.copy(cursor = Some(cursor))
    }

    def recurseIds(collection: UserIds): Future[UserIds] = {
      if (collection.ids.length < 145000 && collection.next_cursor != 0) {
        for {
          nextCollection <- get(resourceWithCursor(collection.next_cursor)) map { r =>
            Parse.decodeOption[UserIds](r.body).getOrElse(UserIds(0, Seq.empty[Long], 0))
          }
          completeCollection <- recurseIds(nextCollection.copy(ids = nextCollection.ids ++ collection.ids))
        } yield completeCollection
      } else Future.successful(collection)
    }

    all match {
      case true => callIds.flatMap(recurseIds)
      case false => callIds
    }
  }

  def userTimeline(resource: StatusesUserTimeline, maxLength: Int = 3200): Future[Seq[Tweet]] = {

    def callTimeline(maxId: Option[Long], count: Int = 200) = {
      val r = resource.copy(maxId = maxId, count = count)
      get(r).map(result => Parse.decodeOr[Seq[Tweet], Seq[Tweet]](result.body, _.seq, Seq.empty[Tweet]))
    }

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
    if (maxLength >= 200) callTimeline(None, 200).flatMap(recurseTimeline) else callTimeline(None, maxLength)
  }
}