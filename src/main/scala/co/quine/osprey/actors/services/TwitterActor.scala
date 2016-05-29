package co.quine.osprey
package actors
package services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalaj.http._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import argonaut._

import co.quine.osprey.twitter._

object TwitterActor {
  def props(requests: ActorRef) = Props(new TwitterActor(requests))
}

class TwitterActor(requests: ActorRef) extends TwitterService with Actor with ActorLogging {

  import twitter.Resources._

  implicit val timeout = Timeout(10.seconds)

  def get(resource: TwitterResource) = {
    for {
      request <- (requests ? resource).mapTo[HttpResponse[String]]
    } yield request
  }

  def receive = {
    case op: TwitterOperation => onOperation(op)
  }

  def onOperation(op: TwitterOperation): Unit = {
    val payload = op.resource match {
      case r: FollowersIds => followersIds(r)
      case r: FollowersList => followersList(r)
      case r: FriendsIds => friendsIds(r)
      case r: FriendsList => friendsList(r)
      case r: StatusesLookup => statusesLookup(r)
      case r: StatusesShow => statusesShow(r)
      case r: StatusesUserTimeline => statusesUserTimeline(r)
      case r: UsersLookup => usersLookup(r)
      case r: UsersShow => usersShow(r)

    }

    payload onSuccess {
      case j: Json => op.client ! op.response(j)
    }
  }

}