package co.quine.osprey
package responses

sealed trait TwitterResponse

case class User(created_at: String,
                description: Option[String],
                favourites_count: Int,
                followers_count: Int,
                friends_count: Int,
                geo_enabled: Boolean,
                id: Long,
                listed_count: Int,
                location: Option[String],
                name: Option[String],
                profile_banner_url: String,
                profile_image_url: String,
                protekted: Boolean,
                screen_name: String,
                status: Option[Tweet],
                statuses_count: Int,
                time_zone: Option[String],
                url: Option[String],
                verified: Boolean) extends TwitterResponse

case class Tweet(coordinates: Option[Point],
                 created_at: String,
                 mentions: List[Long],
                 hashtags: List[String],
                 media: List[Media],
                 urls: List[String],
                 favorite_count: Int,
                 id: Long,
                 in_reply_to_status_id: Option[Long],
                 in_reply_to_user_id: Option[Long],
                 quoted_status_id: Option[Long],
                 retweet_count: Int,
                 retweeted_status: Option[Tweet],
                 text: String,
                 user: Option[User]) extends TwitterResponse

case class Point(longitude: Double, latitude: Double) extends TwitterResponse

case class Media(expanded_url: String,
                 source_status_id: Option[Long],
                 mType: String) extends TwitterResponse

case class UserIds(previous_cursor: Long, ids: Seq[Long], next_cursor: Long) extends TwitterResponse {
  def numCalls = {
    val idLength = ids.length.toDouble
    math.ceil(idLength / 5000).toInt
  }
}

sealed trait IdSet extends TwitterResponse

final case class IdSetComplete(ids: Set[Long]) extends IdSet

final case class IdSetPartial(ttl: Long, ids: Set[Long]) extends IdSet

sealed trait Timeline extends TwitterResponse
final case class CompleteTimeline(statuses: Seq[Tweet]) extends Timeline
final case class PartialTimeline(ttl: Long, statuses: Seq[Tweet]) extends Timeline

case class UserList(previous_cursor: Long, next_cursor: Long, users: Seq[User]) extends TwitterResponse

case class UserTimeline(statuses: Seq[Tweet]) extends TwitterResponse

case class RateLimit(ttl: Long) extends TwitterResponse