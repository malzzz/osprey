package co.quine.osprey
package actors
package services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import argonaut.Argonaut._
import argonaut._

object TwitterActor {
  def props(osprey: ActorRef, requests: ActorRef) = Props(new TwitterActor(osprey, requests))
}

class TwitterActor(osprey: ActorRef, val requests: ActorRef) extends Actor with ActorLogging {

  import codecs.TwitterCodec._
  import resources._
  import responses._

  implicit val timeout = Timeout(10.seconds)

  override def preStart() = context.actorOf(RetryActor.props(), s"${self.path.name}-retries")

  def receive = {
    case TwitterOperation(uuid, resource, client) => onOperation(uuid, resource, client)
    case RetryOperation(op, _, _, retries) =>
  }

  def onOperation(uuid: String, resource: TwitterResource, client: ActorRef, isRetry: Boolean = false) = {
    resource match {
      case r: CursoredIdList => userIds(r) onSuccess {
        case \/-(idSetComplete) => osprey ! Ok(client, uuid, idSetComplete)
        case -\/(idSetPartial) => osprey ! Partial(client, uuid, idSetPartial)
      }
      case r: CursoredList => userList(r) onSuccess {
        case \/-(userSetComplete) => osprey ! Ok(client, uuid, userSetComplete)
        case -\/(userSetPartial) => osprey ! Partial(client, uuid, userSetPartial)
      }
      case r: StatusesUserTimeline => userTimeline(r) onSuccess {
        case \/-(completeTimeline) => osprey ! Ok(client, uuid, completeTimeline)
        case -\/(partialTimeline) => osprey ! Partial(client, uuid, partialTimeline)
      }
      case r: UsersShow => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(Some(user)) => osprey ! Ok(client, uuid, user)
        case -\/(rateLimit) => osprey ! Failed(client, uuid, rateLimit)
      }
      case r: UsersLookup => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(users) => osprey ! Ok(client, uuid, UserSetComplete(users.toSet))
        case -\/(rateLimit) => osprey ! Failed(client, uuid, rateLimit)
      }
      case r: StatusesLookup => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(tweets) => osprey ! Ok(client, uuid, Tweets(tweets))
        case -\/(rateLimit) => osprey ! Failed(client, uuid, rateLimit)
      }
      case r: StatusesShow => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(Some(tweet)) => osprey ! Ok(client, uuid, tweet)
        case -\/(rateLimit) => osprey ! Failed(client, uuid, rateLimit)
      }
    }
  }

  def handleResponse[T](response: HttpStringResponse)(decoder: String => T) = response.r match {
    case r if r.code == 200 =>
      \/-(decoder(r.body))
    case r if r.code == 430 =>
      -\/(Parse.decodeOption[RateLimit](r.body).getOrElse(RateLimit(System.currentTimeMillis / 1000)))
  }

  def requestDecodeResponse[T](resource: TwitterResource)(decoder: String => T) =
    httpRequest(resource).map(handleResponse(_)(decoder))

  def httpRequest(resource: TwitterResource): Future[HttpStringResponse] =
    (requests ? resource).mapTo[HttpStringResponse]

  def userIds(resource: CursoredIdList): Future[IdSetPartial \/ IdSetComplete] = {
    def recurseResource(nextCursor: Long, calls: Int, acc: Set[Long]): Future[IdSet] = {
      if (nextCursor != 0 && calls < resource.maxCalls) {
        for {
          response <- httpRequest(resource.withCursor(nextCursor))
          complete <- handleResponse(response)(Parse.decodeOption[UserIds](_).getOrElse(UserIds(0, Seq.empty[Long], 0))) match {
            case -\/(rateLimit) => Future.successful(IdSetPartial(rateLimit.ttl, acc))
            case \/-(userIds) => recurseResource(userIds.next_cursor, calls + 1, acc ++ userIds.ids)
          }
        } yield complete
      } else Future.successful(IdSetComplete(acc))
    }
    recurseResource(-1, 0, Set.empty[Long]) map {
      case r: IdSetComplete => \/-(r)
      case r: IdSetPartial => -\/(r)
    }
  }

  def userList(resource: CursoredList): Future[UserSetPartial \/ UserSetComplete] = {
    def recurseResource(nextCursor: Long, calls: Int, acc: Set[User]): Future[UserSet] = {
      if (nextCursor != 0 && calls < resource.maxCalls) {
        for {
          response <- httpRequest(resource.withCursor(nextCursor))
          complete <- handleResponse(response)(Parse.decodeOption[UserList](_).getOrElse(UserList(0, 0, Seq.empty[User]))) match {
            case -\/(rateLimit) => Future.successful(UserSetPartial(rateLimit.ttl, acc))
            case \/-(users) => recurseResource(users.next_cursor, calls + 1, acc ++ users.users)
          }
        } yield complete
      } else Future.successful(UserSetComplete(acc))
    }
    recurseResource(-1, 0, Set.empty[User]) map {
      case r: UserSetComplete => \/-(r)
      case r: UserSetPartial => -\/(r)
    }
  }

  def userTimeline(resource: StatusesUserTimeline): Future[PartialTimeline \/ CompleteTimeline] = {
    def recurseTimeline(t: Seq[Tweet], calls: Int = 0): Future[Timeline] = {
      val maxLength = resource.count
      val maxCalls = maxLength / 200
      val userStatusCount = t.head.user.map(_.statuses_count).getOrElse(0)
      val statusesRemaining = {
        val absDiff = userStatusCount - t.length
        if (absDiff > 3200) 3200 else absDiff
      }

      if (statusesRemaining > 0 && t.length < maxLength && calls < maxCalls) {
        val countThisCall = if (statusesRemaining > 200) 200 else statusesRemaining
        val maxId = t.map(tweets => tweets.id).min - 1

        for {
          nextTimeline <- httpRequest(resource.withMaxId(maxId, countThisCall))
          completeTimeline <- handleResponse(nextTimeline)(Parse.decodeOption[Seq[Tweet]](_).getOrElse(Seq.empty[Tweet])) match {
            case -\/(rateLimit) => Future.successful(PartialTimeline(rateLimit.ttl, t))
            case \/-(timeline) => recurseTimeline(t ++ timeline, calls = calls + 1)
          }
        } yield completeTimeline
      } else Future.successful(CompleteTimeline(t))
    }

    def buildTimeline = for {
      firstCall <- httpRequest(resource.withCount(200))
      recursiveCall <- handleResponse(firstCall)(Parse.decodeOption[Seq[Tweet]](_).getOrElse(Seq.empty[Tweet])) match {
        case -\/(rateLimit) => Future.successful(PartialTimeline(rateLimit.ttl, Seq.empty[Tweet]))
        case \/-(tweets) if tweets.nonEmpty => recurseTimeline(tweets, 1)
        case _ => Future.successful(CompleteTimeline(Seq.empty[Tweet]))
      }
    } yield recursiveCall

    buildTimeline map {
      case r: CompleteTimeline => \/-(r)
      case r: PartialTimeline => -\/(r)
    }
  }

  def userHashtags(cmd: UserHashtags) = {
    val resource = StatusesUserTimeline(cmd.id, cmd.screenName, count = 3200)
    val timeline = userTimeline(resource) map {
      case \/-(completeTimeline) => completeTimeline.statuses
      case -\/(partialTimeline) => partialTimeline.statuses
    }

    timeline map { t =>
      t.foldLeft(Seq.empty[String])((x, tweet) => x ++ tweet.hashtags).groupBy(identity).mapValues(_.size)
    }
  }

}