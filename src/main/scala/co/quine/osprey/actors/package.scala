package co.quine.osprey

import akka.actor._
import argonaut._

package object actors {

  import twitter.Resources._

  trait Operation[T <: Operation[T]] { self: T =>
    val uuid: String
    val resource: T
    val eta: Option[String]
    val client: ActorRef
  }

  case class TwitterOperation(uuid: String, resource: TwitterResource, eta: Option[String], client: ActorRef) {
    def response(payload: Json) = Response(uuid, payload)
  }

  trait ResponseObject

  case class Response(uuid: String, payload: Json)

}