package co.quine.osprey.twitter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import argonaut.Argonaut._
import argonaut._

trait Api { self: TwitterService =>

  import Codec._
  import Resources._

  def followersIds(resource: FollowersIds): Future[Json] = usersIds(resource).map(r => r.asJson)

  def followersList(resource: FollowersList): Future[Json] = get(resource) map { r =>
    val userList = Parse.decodeOption[UserList](r.body).getOrElse(UserList(0, 0, Seq.empty[User]))
    userList.asJson
  }

  def friendsIds(resource: FriendsIds): Future[Json] = usersIds(resource).map(r => r.asJson)

  def friendsList(resource: FriendsList): Future[Json] = get(resource) map { r =>
    val userList = Parse.decodeOption[UserList](r.body).getOrElse(UserList(0, 0, Seq.empty[User]))
    userList.asJson
  }

  def statusesLookup(resource: StatusesLookup): Future[Json] = get(resource) map { r =>
    val statuses = Parse.decodeOr[Seq[Tweet], Seq[Tweet]](r.body, _.seq, Seq.empty[Tweet])
    statuses.asJson
  }

  def statusesShow(resource: StatusesShow): Future[Json] = get(resource) map { r =>
    val status = Parse.decodeOption[Tweet](r.body)
    status.asJson
  }

  def statusesUserTimeline(resource: StatusesUserTimeline): Future[Json] =
    userTimeline(resource, resource.count).map(r => r.asJson)


  def usersLookup(resource: UsersLookup): Future[Json] = get(resource) map { r =>
    val users = Parse.decodeOption[Seq[User]](r.body).getOrElse(Seq.empty[User])
    users.asJson
  }

  def usersShow(resource: UsersShow): Future[Json] = get(resource) map { r =>
    val user = Parse.decodeOption[User](r.body)
    user.asJson
  }

}