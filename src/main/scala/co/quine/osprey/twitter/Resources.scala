package co.quine.osprey.twitter

import akka.actor._
import argonaut._
import Argonaut._
import scalaj.http.{Http, HttpRequest, HttpResponse, Token => HttpToken}
import scala.collection.{GenTraversable, mutable}

import co.quine.gatekeeperclient.GatekeeperClient._
import co.quine.gatekeeperclient._

object Resources {

  implicit class HttpRequests(request: HttpRequest) {
    def sign(implicit gate: GatekeeperClient) = {
      val resource = request.url.diff("https://api.twitter.com/1.1")
      val token = resource match {
        case "/users/show.json" => gate.usersShow
        case "/users/lookup.json" => gate.usersLookup
        case "/statuses/lookup.json" => gate.statusesLookup
        case "/statuses/show.json" => gate.statusesShow
        case "/statuses/user_timeline.json" => gate.statusesUserTimeline
        case "/friends/ids.json" => gate.friendsIds
        case "/friends/list.json" => gate.friendsList
        case "/followers/ids.json" => gate.followersIds
        case "/followers/list.json" => gate.followersList
      }

      val response: HttpResponse[String] = token match {
        case AccessToken(k, s) =>
          val consumer = gate.defaultConsumer
          request.oauth(HttpToken(consumer.key, consumer.secret), HttpToken(k, s)).asString
        case BearerToken(k) => request.header("Authorization", s"Bearer $k").asString
        case Unavailable(ttl) =>
          HttpResponse[String](s"""{"rate_limit": $ttl}""", 430, Map("TTL" -> IndexedSeq(ttl.toString)))
      }

      if (response.is2xx) {
        val remaining = response.header("x-rate-limit-remaining")
        val reset = response.header("x-rate-limit-reset")

        if (remaining.nonEmpty && reset.nonEmpty) {
          gate.updateRateLimit(token.key, resource, remaining.get.toInt, reset.get.toLong)
        }
      }
      response
    }
  }
}