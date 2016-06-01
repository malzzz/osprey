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
  def props(requests: ActorRef) = Props(new TwitterActor(requests))
}

class TwitterActor(val requests: ActorRef) extends Actor with ActorLogging {

  import codecs.TwitterCodec._
  import resources._
  import responses._

  implicit val timeout = Timeout(10.seconds)

  def receive = {
    case op@TwitterOperation(uuid, resource, client, retries) => onOperation(uuid, resource, client)
  }

  def onOperation(uuid: String, resource: TwitterResource, client: ActorRef) = {
    resource match {
      case r: CursoredResource => userIds(r) onSuccess {
        case \/-(idSetComplete) => client ! Ok(uuid, idSetComplete.asJson)
        case -\/(idSetPartial) => client ! Partial(uuid, idSetPartial.asJson)
      }
      case r: StatusesUserTimeline => userTimeline(r) onSuccess {
        case \/-(completeTimeline) => client ! Ok(uuid, completeTimeline.asJson)
        case -\/(partialTimeline) => client ! Partial(uuid, partialTimeline.asJson)
      }
      case r: UsersShow => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(Some(user)) => client ! Ok(uuid, user.asJson)
        case -\/(rateLimit) => client ! Failed(uuid, rateLimit.asJson)
      }
      case r: UsersLookup => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(users) => client ! Ok(uuid, users.asJson)
        case -\/(rateLimit) => client ! Failed(uuid, rateLimit.asJson)
      }
      case r: StatusesLookup => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(tweets) => client ! Ok(uuid, tweets.asJson)
        case -\/(rateLimit) => client ! Failed(uuid, rateLimit.asJson)
      }
      case r: StatusesShow => requestDecodeResponse(r)(r.decode) onSuccess {
        case \/-(Some(tweet)) => client ! Ok(uuid, tweet.asJson)
        case -\/(rateLimit) => client ! Failed(uuid, rateLimit.asJson)
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

  def userIds(resource: CursoredResource): Future[IdSetPartial \/ IdSetComplete] = {
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

  def reschedule(op: TwitterOperation, when: Long) = {
    val timeDiff = when - (System.currentTimeMillis / 1000)
    context.system.scheduler.scheduleOnce(timeDiff.seconds, self, op)
  }

  def userTimeline(resource: StatusesUserTimeline): Future[PartialTimeline \/ CompleteTimeline] = {
    def recurseTimeline(t: Seq[Tweet]): Future[Timeline] = {
      val maxLength = resource.count
      val userStatusCount = t.head.user.map(_.statuses_count).getOrElse(0)
      val statusesRemaining = {
        val absDiff = userStatusCount - t.length
        if (absDiff > 3200) 3200 else absDiff
      }

      if (statusesRemaining > 0 && t.length < maxLength) {
        val countThisCall = if (statusesRemaining > 200) 200 else statusesRemaining
        val maxId = t.map(tweets => tweets.id).min - 1

        for {
          nextTimeline <- httpRequest(resource.withMaxId(maxId, countThisCall))
          completeTimeline <- handleResponse(nextTimeline)(Parse.decodeOption[Seq[Tweet]](_).getOrElse(Seq.empty[Tweet])) match {
            case -\/(rateLimit) => Future.successful(PartialTimeline(rateLimit.ttl, t))
            case \/-(timeline) => recurseTimeline(t ++ timeline)
          }
        } yield completeTimeline
      } else Future.successful(CompleteTimeline(t))
    }

    def buildTimeline = for {
      firstCall <- httpRequest(resource.withCount(200))
      recursiveCall <- handleResponse(firstCall)(Parse.decodeOption[Seq[Tweet]](_).getOrElse(Seq.empty[Tweet])) match {
        case -\/(rateLimit) => Future.successful(PartialTimeline(rateLimit.ttl, Seq.empty[Tweet]))
        case \/-(tweets) if tweets.nonEmpty => recurseTimeline(tweets)
        case _ => Future.successful(CompleteTimeline(Seq.empty[Tweet]))
      }
    } yield recursiveCall

    buildTimeline map {
      case r: CompleteTimeline => \/-(r)
      case r: PartialTimeline => -\/(r)
    }
  }

}