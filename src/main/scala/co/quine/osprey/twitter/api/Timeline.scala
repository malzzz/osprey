package co.quine.osprey.twitter
package api

class Timeline(userId: Option[Long] = None, screenName: Option[String] = None) {

  require(userId.isDefined || screenName.isDefined, "Must specify either user_id or screen_name")





}