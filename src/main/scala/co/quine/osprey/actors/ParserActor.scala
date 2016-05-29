package co.quine.osprey
package actors

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

import akka.actor._
import akka.util.{ByteString, ByteStringBuilder}
import argonaut.Argonaut._
import argonaut._

import co.quine.osprey.twitter.Resources._

object ParserActor {
  case class ClientRequest(uuid: String, resource: TwitterResource, eta: String)

  implicit def ClientRequestDecodeJson: DecodeJson[ClientRequest] =
    DecodeJson(c => for {
      uuid <- (c --\ "uuid").as[String]
      service <- (c --\ "service").as[String]
      method <- (c --\ "method").as[String]
      resource <- method.toLowerCase match {
        case "ushow" => for {
          id <- (c --\ "args" --\ "id").as[Option[Long]]
          screenName <- (c --\ "args" --\ "screen_name").as[Option[String]]
        } yield UsersShow(id, screenName)
        case "ulookup" => for {
          id <- (c --\ "args" --\ "id").as[Option[Seq[Long]]]
          screenName <- (c --\ "args" --\ "screen_name").as[Option[Seq[String]]]
        } yield UsersLookup(id.getOrElse(Seq.empty[Long]), screenName.getOrElse(Seq.empty[String]))
        case "slookup" => for {
          id <- (c --\ "args" --\ "id").as[Seq[Long]]
        } yield StatusesLookup(id)
        case "sshow" => for {
          id <- (c --\ "args" --\ "id").as[Long]
        } yield StatusesShow(id)
        case "susertimeline" => for {
          id <- (c --\ "args" --\ "id").as[Option[Long]]
          screenName <- (c --\ "args" --\ "screen_name").as[Option[String]]
          sinceId <- (c --\ "args" --\ "since_id").as[Option[Long]]
          maxId <- (c --\ "args" --\ "max_id").as[Option[Long]]
          count <- (c --\ "args" --\ "count").as[Int]
        } yield StatusesUserTimeline(id, screenName, sinceId, maxId, count)
        case "frids" => for {
          id <- (c --\ "args" --\ "id").as[Option[Long]]
          screenName <- (c --\ "args" --\ "screen_name").as[Option[String]]
          all <- (c --\ "args" --\ "all").as[Boolean]
        } yield FriendsIds(id, screenName, None, all)
        case "frlist" => for {
          id <- (c --\ "args" --\ "id").as[Option[Long]]
          screenName <- (c --\ "args" --\ "screen_name").as[Option[String]]
        } yield FriendsList(id, screenName)
        case "foids" => for {
          id <- (c --\ "args" --\ "id").as[Option[Long]]
          screenName <- (c --\ "args" --\ "screen_name").as[Option[String]]
          all <- (c --\ "args" --\ "all").as[Boolean]
        } yield FollowersIds(id, screenName, None, all)
        case "folist" => for {
          id <- (c --\ "args" --\ "id").as[Option[Long]]
          screenName <- (c --\ "args" --\ "screen_name").as[Option[String]]
        } yield FollowersList(id, screenName)
      }
      eta <- (c --\ "eta").as[String]
    } yield ClientRequest(uuid, resource, eta))

  def props(client: ActorRef) = Props(new ParserActor(client))
}

class ParserActor(client: ActorRef) extends Actor with ActorLogging {

  import ParserActor._

  val buffer = ArrayBuffer[Byte]()

  def receive = {
    case bs: ByteString => onData(bs)
  }

  def onData(bs: ByteString) = {
    buffer ++= bs
    buffer.lastIndexOfSlice("\r\n") match {
      case x: Int if x != -1 =>
        val toParse = buffer.slice(0, x)
        buffer.trimStart(toParse.length + 2)
        val parsed = parse(toParse)
        onParsed(parsed)
      case _ =>
    }
  }

  def onParsed(l: List[ByteString]) = l foreach { bs =>
    Parse.decodeOption[ClientRequest](bs.utf8String).foreach(client ! _)
  }

  def parse(a: ArrayBuffer[Byte]): List[ByteString] = {
    @tailrec def loop(a: ArrayBuffer[Byte], acc: List[ByteString]): List[ByteString] = a.head match {
      case '$' =>
        val len = parseLength(a.tail)
        val lenLength = len.toString.length + 2

        val payload = {
          if (a.length < lenLength + len) {
            ByteString.empty
          } else {
            val bs = new ByteStringBuilder() ++= a.slice(len.toString.length + 2, len + 1)
            bs.result
          }
        }
        loop(a.drop(lenLength + len), acc :+ payload)
      case '\r' | '\n' => loop(a.tail, acc)
      case _ => acc
    }
    loop(a, List.empty[ByteString])
  }

  def parseLength(a: ArrayBuffer[Byte]): Int = {
    a.takeWhile(_ != '\r').foldLeft(0)((len: Int, p: Byte) => (len * 10) + (p - '0'))
  }
}