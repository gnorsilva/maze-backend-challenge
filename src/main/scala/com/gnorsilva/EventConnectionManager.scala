package com.gnorsilva

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.util.ByteString

class EventConnectionManager(tcpManager: ActorRef, eventProcessor: ActorRef) extends Actor with ActorLogging {

  tcpManager ! Bind(self, new InetSocketAddress("localhost", 9090))

  override def receive: Receive = {
    case Bound(local) =>
      log.info(s"Server started on $local")
    case Connected(remote, local) =>
      log.info(s"New connnection: $local -> $remote")
      sender() ! Register(self)
    case Received(data) =>
      val events = parseDataToEvents(data)
      if (events.nonEmpty)
        eventProcessor ! EventBatch(events)
  }

  private def parseDataToEvents(data: ByteString) = {
    data.utf8String
      .lines
      .map(parseEvent)
      .filter(_ != InvalidEvent)
      .toSeq
  }

  private def parseEvent: String => Event = { eventData: String =>
    val items: Array[String] = eventData.split("\\|")
    if (items.length <= 1) {
      InvalidEvent
    } else {
      items(1) match {
        case "F" => Follow(items(0).toInt, eventData, items(2).toInt, items(3).toInt)
        case "U" => Unfollow(items(0).toInt, eventData, items(2).toInt, items(3).toInt)
        case "B" => Broadcast(items(0).toInt, eventData)
        case "P" => PrivateMessage(items(0).toInt, eventData, items(2).toInt, items(3).toInt)
        case "S" => StatusUpdates(items(0).toInt, eventData, items(2).toInt)
        case _ => InvalidEvent
      }
    }
  }

}

case class EventBatch(events: Seq[Event])

trait Event {
  def id: Int
  def message: String
}

object InvalidEvent extends Event {
  override def id: Int = -1

  override def message: String = "invalid"
}

object Event {
  implicit def orderingById[A <: Event]: Ordering[A] = Ordering.by(_.id)
}

case class Follow(override val id: Int, message: String, from: Int, to: Int) extends Event

case class Unfollow(override val id: Int, message: String, from: Int, to: Int) extends Event

case class Broadcast(override val id: Int, message: String) extends Event

case class PrivateMessage(override val id: Int, message: String, from: Int, to: Int) extends Event

case class StatusUpdates(override val id: Int, message: String, from: Int) extends Event


