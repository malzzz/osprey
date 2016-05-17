package co.quine.osprey.twitter

import akka.actor._
import scalaj.http._

import scala.concurrent.Promise

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

  def get(resource: Resources.TwitterResource) = {
    val promise = Promise[HttpResponse[String]]()
    requests ! GetRequest(resource, promise)
    promise.future
  }
}