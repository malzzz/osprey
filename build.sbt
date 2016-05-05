name := "osprey"

organization := "co.quine"

version := "0.0.1"

scalaVersion := "2.11.8"

isSnapshot := true

publishTo := Some("Quine snapshots" at "s3://snapshots.repo.quine.co")

resolvers ++= Seq[Resolver](
  "Quine Releases"                   at "s3://releases.repo.quine.co",
  "Quine Snapshots"                  at "s3://snapshots.repo.quine.co",
  "Typesafe repository snapshots"    at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases"     at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo"                    at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases"                at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots"               at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype staging"                 at "http://oss.sonatype.org/content/repositories/staging",
  "Sonatype release Repository"      at "http://oss.sonatype.org/service/local/staging/deploy/maven2/",
  "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/",
  "Spray repo"                       at "http://repo.spray.io"
)

lazy val versions = new {
  val akka = "2.4.3"
  val akkaKafka = "0.11-M2"
  val nscalatime = "2.12.0"
  val config = "1.3.0"
  val scalalogging = "3.1.0"
  val cassandra = "3.0.0"
  val redis = "1.6.0"
  val scredis = "2.0.6"
  val spray = "1.3.3"
  val gatekeeperclient = "0.0.3"
}

libraryDependencies ++= Seq(
  "co.quine"                    %% "gatekeeper-client" % versions.gatekeeperclient changing(),
  "com.github.nscala-time"      %% "nscala-time" % versions.nscalatime,
  "com.github.etaty"            %% "rediscala" % versions.redis,
  "com.typesafe"                 % "config" % versions.config,
  "com.typesafe.scala-logging"  %% "scala-logging" % versions.scalalogging,
  "com.typesafe.akka"           %% "akka-actor" % versions.akka,
  "com.typesafe.akka"           %% "akka-agent" % versions.akka,
  "com.typesafe.akka"           %% "akka-cluster" % versions.akka,
  "com.typesafe.akka"           %% "akka-kernel" % versions.akka,
  "com.typesafe.akka"           %% "akka-persistence" % versions.akka,
  "com.typesafe.akka"           %% "akka-stream" % versions.akka,
  "com.typesafe.akka"           %% "akka-http-core" % versions.akka,
  "com.typesafe.akka"           %% "akka-http-experimental" % versions.akka,
  "com.typesafe.akka"           %% "akka-http-spray-json-experimental" % versions.akka,
  "com.typesafe.akka"           %% "akka-stream-kafka" % versions.akkaKafka,
  "com.typesafe.akka"           %% "akka-slf4j" % versions.akka,
  "ch.qos.logback"               % "logback-classic" % "1.1.3",
  "io.spray"                    %% "spray-json" % "1.3.2",
  "commons-codec"                % "commons-codec" % "1.10",
  "io.spray"                    %% "spray-http" % versions.spray,
  "io.spray"                    %% "spray-httpx" % versions.spray,
  "io.spray"                    %% "spray-caching" % versions.spray,
  "io.spray"                    %% "spray-can" % versions.spray,
  "io.spray"                    %% "spray-client" % versions.spray,
  "io.spray"                    %% "spray-util" % versions.spray,
  "org.parboiled"                % "parboiled-core" % "1.1.7",
  "org.scalaj"                  %% "scalaj-http" % "2.3.0",
  "io.argonaut"                 %% "argonaut" % "6.1"
)

evictionWarningOptions in update := EvictionWarningOptions.default
  .withWarnTransitiveEvictions(false)
  .withWarnDirectEvictions(false)
  .withWarnScalaVersionEviction(false)
    