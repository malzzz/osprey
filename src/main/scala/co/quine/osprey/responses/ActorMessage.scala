package co.quine.osprey
package responses

import scalaj.http._

sealed trait ActorMessage
case class HttpStringResponse(r: HttpResponse[String]) extends ActorMessage