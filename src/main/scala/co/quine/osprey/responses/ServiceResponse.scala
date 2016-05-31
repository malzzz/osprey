package co.quine.osprey
package responses

import argonaut._

sealed trait ServiceResponse {
  val uuid: String
  val response: Json
}
case class Ok(uuid: String, response: Json) extends ServiceResponse
case class Partial(uuid: String, response: Json) extends ServiceResponse
case class Failed(uuid: String, response: Json) extends ServiceResponse