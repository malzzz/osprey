package co.quine.osprey
package codecs

import argonaut._, Argonaut._
import monocle._, Monocle._

object TwitterCodec {

  import responses._

  implicit def RateLimitCodecJson: CodecJson[RateLimit] =
    CodecJson(
      (r: RateLimit) =>
        ("ttl" := r.ttl) ->:
          jEmptyObject,
      c => for {
        ttl <- (c --\ "rate_limit").as[Long]
      } yield RateLimit(ttl))

  implicit def UserCodecJson: CodecJson[User] =
    CodecJson(
      (u: User) =>
        ("created_at" := u.created_at) ->:
          ("description" := u.description) ->:
          ("favourites_count" := u.favourites_count) ->:
          ("followers_count" := u.followers_count) ->:
          ("friends_count" := u.friends_count) ->:
          ("geo_enabled" := u.geo_enabled) ->:
          ("id" := u.id) ->:
          ("listed_count" := u.listed_count) ->:
          ("location" := u.location) ->:
          ("name" := u.name) ->:
          ("profile_banner_url" := u.profile_banner_url) ->:
          ("profile_image_url" := u.profile_image_url) ->:
          ("protected" := u.protekted) ->:
          ("screen_name" := u.screen_name) ->:
          ("status" := u.status) ->:
          ("statuses_count" := u.statuses_count) ->:
          ("time_zone" := u.time_zone) ->:
          ("url" := u.url) ->:
          ("verified" := u.verified) ->:
          jEmptyObject,
      c => for {
        created_at <- (c --\ "created_at").as[String]
        description <- (c --\ "description").as[Option[String]]
        favourites_count <- (c --\ "favourites_count").as[Int]
        followers_count <- (c --\ "followers_count").as[Int]
        friends_count <- (c --\ "friends_count").as[Int]
        geo_enabled <- (c --\ "geo_enabled").as[Boolean]
        id <- (c --\ "id").as[Long]
        listed_count <- (c --\ "listed_count").as[Int]
        location <- (c --\ "location").as[Option[String]]
        name <- (c --\ "name").as[Option[String]]
        profile_banner_url <- (c --\ "profile_banner_url").as[String]
        profile_image_url <- (c --\ "profile_image_url").as[String]
        protekted <- (c --\ "protected").as[Boolean]
        screen_name <- (c --\ "screen_name").as[String]
        status <- (c --\ "status").as[Option[Tweet]]
        statuses_count <- (c --\ "statuses_count").as[Int]
        time_zone <- (c --\ "time_zone").as[Option[String]]
        url <- (c --\ "url").as[Option[String]]
        verified <- (c --\ "verified").as[Boolean]
      } yield User(
        created_at,
        description,
        favourites_count,
        followers_count,
        friends_count,
        geo_enabled,
        id,
        listed_count,
        location,
        name,
        profile_banner_url,
        profile_image_url,
        protekted,
        screen_name,
        status,
        statuses_count,
        time_zone,
        url,
        verified))

  implicit def TweetCodecJson: CodecJson[Tweet] = {

    val hashtagLens = jObjectPrism
      .composeOptional(index("hashtags"))
      .composePrism(jArrayPrism)
      .composeTraversal(each[List[Json], Json])
      .composePrism(jObjectPrism)
      .composeOptional(index("text"))
      .composePrism(jStringPrism)

    val mentionLens = jObjectPrism
      .composeOptional(index("user_mentions"))
      .composePrism(jArrayPrism)
      .composeTraversal(each[List[Json], Json])
      .composePrism(jObjectPrism)
      .composeOptional(index("id"))
      .composePrism(jLongPrism)

    val urlLens = jObjectPrism
      .composeOptional(index("urls"))
      .composePrism(jArrayPrism)
      .composeTraversal(each[List[Json], Json])
      .composePrism(jObjectPrism)
      .composeOptional(index("expanded_url"))
      .composePrism(jStringPrism)

    CodecJson(
      (t: Tweet) =>
        ("coordinates" := t.coordinates.map(x => Seq(x.longitude, x.latitude))) ->:
          ("created_at" := t.created_at) ->:
          ("hashtags" := t.hashtags) ->:
          ("mentions" := t.mentions) ->:
          ("media" := t.media) ->:
          ("urls" := t.urls) ->:
          ("favorite_count" := t.favorite_count) ->:
          ("id" := t.id) ->:
          ("in_reply_to_status_id" := t.in_reply_to_status_id) ->:
          ("in_reply_to_user_id" := t.in_reply_to_user_id) ->:
          ("quoted_status_id" := t.quoted_status_id) ->:
          ("retweet_count" := t.retweet_count) ->:
          ("retweeted_status" := t.retweeted_status) ->:
          ("text" := t.text) ->:
          ("user" := t.user) ->:
          jEmptyObject,
      c => for {
        coordinates <- (c --\ "coordinates").as[Option[Point]]
        created_at <- (c --\ "created_at").as[String]
        entities <- (c --\ "entities").as[Json]
        media <- (c --\ "entities" --\ "media").as[Option[List[Media]]]
        favorite_count <- (c --\ "favorite_count").as[Int]
        id <- (c --\ "id").as[Long]
        in_reply_to_status_id <- (c --\ "in_reply_to_status_id").as[Option[Long]]
        in_reply_to_user_id <- (c --\ "in_reply_to_user_id").as[Option[Long]]
        quoted_status_id <- (c --\ "quoted_status_id").as[Option[Long]]
        retweet_count <- (c --\ "retweet_count").as[Int]
        retweeted_status <- (c --\ "retweeted_status").as[Option[Tweet]]
        text <- (c --\ "text").as[String]
        user <- (c --\ "user").as[Option[User]]
      } yield Tweet(
        coordinates,
        created_at,
        mentionLens.getAll(entities),
        hashtagLens.getAll(entities),
        media.getOrElse(List.empty[Media]),
        urlLens.getAll(entities),
        favorite_count,
        id,
        in_reply_to_status_id,
        in_reply_to_user_id,
        quoted_status_id,
        retweet_count,
        retweeted_status,
        text,
        user))
  }

  implicit def PointDecodeJson: DecodeJson[Point] =
    DecodeJson(c => for {
      longlat <- (c --\ "coordinates").as[Seq[Double]]
    } yield longlat match { case Seq(long, lat) => Point(long, lat) })

  implicit def MediaCodecJson: CodecJson[Media] =
    CodecJson(
      (m: Media) =>
        ("expanded_url" := m.expanded_url) ->:
          ("source_status_id" := m.source_status_id) ->:
          ("type" := m.mType) ->:
          jEmptyObject,
      c => for {
        expanded_url <- (c --\ "expanded_url").as[String]
        source_status_id <- (c --\ "source_status_id").as[Option[Long]]
        mType <- (c --\ "type").as[String]
      } yield Media(expanded_url, source_status_id, mType))

  implicit def UserIdsCodecJson: CodecJson[UserIds] =
    CodecJson(
      (u: UserIds) =>
        ("ids" := u.ids) ->:
          jEmptyObject,
      c => for {
        previous_cursor <- (c --\ "previous_cursor").as[Long]
        ids <- (c --\ "ids").as[Seq[Long]]
        next_cursor <- (c --\ "next_cursor").as[Long]
      } yield UserIds(previous_cursor, ids, next_cursor))

  implicit def IdSetCompleteEncodeJson: EncodeJson[IdSetComplete] =
    EncodeJson((i: IdSetComplete) => ("ids" := i.ids) ->: jEmptyObject)

  implicit def IdSetPartialEncodeJson: EncodeJson[IdSetPartial] =
    EncodeJson((i: IdSetPartial) => ("ttl" := i.ttl) ->: ("ids" := i.ids) ->: jEmptyObject)

  implicit def UserSetCompleteEncodeJson: EncodeJson[UserSetComplete] =
    EncodeJson((i: UserSetComplete) => ("users" := i.users) ->: jEmptyObject)

  implicit def UserSetPartialEncodeJson: EncodeJson[UserSetPartial] =
    EncodeJson((i: UserSetPartial) => ("ttl" := i.ttl) ->: ("users" := i.users) ->: jEmptyObject)

  implicit def CompleteTimelineEncodeJson: EncodeJson[CompleteTimeline] =
    EncodeJson((t: CompleteTimeline) => ("statuses" := t.statuses) ->: jEmptyObject)

  implicit def PartialTimelineEncodeJson: EncodeJson[PartialTimeline] =
    EncodeJson((t: PartialTimeline) => ("ttl" := t.ttl) ->: ("statuses" := t.statuses) ->: jEmptyObject)

  implicit def UserListCodecJson: CodecJson[UserList] =
    CodecJson(
      (u: UserList) =>
        ("users" := u.users) ->:
          jEmptyObject,
      c => for {
        previous_cursor <- (c --\ "previous_cursor").as[Long]
        next_cursor <- (c --\ "next_cursor").as[Long]
        users <- (c --\ "users").as[Seq[User]]
      } yield UserList(previous_cursor, next_cursor, users))
}