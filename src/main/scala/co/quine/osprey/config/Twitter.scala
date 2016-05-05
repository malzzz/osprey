package co.quine.osprey.config

import com.typesafe.config.ConfigFactory

object Twitter {

  private val config = ConfigFactory.load()
  private lazy val root = config.getConfig("osprey")

  private val twitterConfig = root.getConfig("twitter")

  lazy val twitterHost = twitterConfig.getString("twitter-host")
  lazy val twitterScheme = twitterConfig.getString("twitter-scheme")
  lazy val twitterVersion = twitterConfig.getString("twitter-version")
  lazy val preamble = s"$twitterScheme://$twitterHost/$twitterVersion"

}