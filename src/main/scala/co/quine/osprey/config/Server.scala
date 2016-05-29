package co.quine.osprey.config

import com.typesafe.config.ConfigFactory

object Server {

  private val config = ConfigFactory.load()
  private lazy val root = config.getConfig("osprey")

  private val serverConfig = root.getConfig("server")

  lazy val host = serverConfig.getString("host")
  lazy val port = serverConfig.getInt("port")

}