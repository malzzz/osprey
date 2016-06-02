package co.quine.osprey.config

import com.typesafe.config.ConfigFactory

object Backends {

  private val config = ConfigFactory.load()
  private lazy val root = config.getConfig("osprey")

  private val backendsConfig = root.getConfig("backends")

  lazy val cassandraNode = backendsConfig.getString("cassandra-node")

}