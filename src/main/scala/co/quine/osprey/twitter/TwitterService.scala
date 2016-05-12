package co.quine.osprey.twitter

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scalaj.http._

import scala.concurrent.duration._

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

  def get(resource: Resources.TwitterResource) = requests ? GetRequest(resource) collect {
    case response: HttpResponse[String] => response.body
  }
}