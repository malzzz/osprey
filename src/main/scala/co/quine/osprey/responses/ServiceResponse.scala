package co.quine.osprey
package responses

import akka.actor._

sealed trait ServiceResponse extends Product with Serializable {
  val client: ActorRef
  val uuid: String
  val response: Response
  val status: String
}

case class Ok(client: ActorRef, uuid: String, response: Response) extends ServiceResponse {
  val status = "ok"
}

case class Partial(client: ActorRef, uuid: String, response: Response) extends ServiceResponse {
  val status = "partial"
}

case class Failed(client: ActorRef, uuid: String, response: Response) extends ServiceResponse {
  val status = "failed"
}