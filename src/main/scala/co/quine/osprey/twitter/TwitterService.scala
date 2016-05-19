package co.quine.osprey.twitter

import akka.actor._
import argonaut._
import scalaj.http._
import scalaz._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import co.quine.gatekeeperclient.GatekeeperClient.Unavailable
import co.quine.osprey._
import co.quine.osprey.actors._
import co.quine.osprey.twitter.api._

trait TwitterService
  extends Followers
    with Friends
    with Statuses
    with Users {
  self: Osprey =>

  import HttpRequestActor._
  import Codec._
  import Resources._

  def get(resource: Resources.TwitterResource) = {
    val promise = Promise[HttpResponse[String]]()
    requests ! GetRequest(resource, promise)
    promise.future
  }

  def onResponse(futureResponse: Future[HttpResponse[String]]): Future[Unavailable \/ String] = {
    val response: Future[Unavailable \/ String] = futureResponse map {
      case r: HttpResponse[String] if r.code == 200 => \/-(r.body)
      case r: HttpResponse[String] if r.code == 430 =>
        -\/(Parse.decodeOption[Unavailable](r.body).getOrElse(Unavailable(System.currentTimeMillis / 1000)))
    }
    response
  }

  def usersIds(resource: TwitterResource,
               all: Boolean = false) = {

    require(resource.isInstanceOf[FriendsIds] || resource.isInstanceOf[FollowersIds], "Invalid resource provided")

    def callIds = get(resource) map { r =>
      Parse.decodeOption[UserIds](r.body).getOrElse(UserIds(0, Seq.empty[Long], 0))
    }

    def resourceWithCursor(cursor: Long) = resource match {
      case r: FriendsIds => r.copy(params = resource.params + ("cursor" -> cursor.toString))
      case r: FollowersIds => r.copy(params = resource.params + ("cursor" -> cursor.toString))
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

}