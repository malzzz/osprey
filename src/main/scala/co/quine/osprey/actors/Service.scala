package co.quine.osprey.actors

sealed trait Service
case object Twitter extends Service