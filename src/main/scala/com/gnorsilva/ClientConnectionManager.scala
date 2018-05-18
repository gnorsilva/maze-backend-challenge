package com.gnorsilva

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.util.ByteString

import scala.collection.mutable
import scala.util.Try

class ClientConnectionManager(tcpManager: ActorRef) extends Actor with ActorLogging {

  tcpManager ! Bind(self, new InetSocketAddress("localhost", 9099))

  val connections: mutable.HashMap[Int, ActorRef] = mutable.HashMap()

  override def receive: Receive = {
    case Bound(local) =>
      log.info(s"Server started on $local")
    case Connected(remote, local) =>
      log.info(s"New connnection: $local -> $remote")
      sender() ! Register(self)
    case Received(data) =>
      parseClientId(data).foreach(id => connections += (id -> sender))
    case ClientEvents(events) =>
      dispatch(events)
    case closed: ConnectionClosed =>
      removeConnection(sender)
  }

  private def parseClientId(data: ByteString): Option[Int] = {
    Try(data.utf8String.stripLineEnd.trim.toInt) toOption
  }

  def dispatch(events: Seq[ClientEvent]): Unit = events.foreach {
    case message: ClientMessage =>
      connections.get(message.clientId).foreach(_ ! Write(ByteString(s"${message.payload}\r\n")))
    case broadcast: ClientBroadcast =>
      connections.foreach((t: (Int, ActorRef)) => t._2 ! Write(ByteString(s"${broadcast.payload}\r\n")))
  }

  private def removeConnection(connection: ActorRef) = {
    connections
      .find((t: (Int, ActorRef)) => t._2 == connection)
      .foreach((t: (Int, ActorRef)) => connections -= t._1)
  }
}

trait ClientEvent

case class ClientMessage(clientId: Int, payload: String) extends ClientEvent

case class ClientBroadcast(payload: String) extends ClientEvent

case class ClientEvents(events: Seq[ClientEvent])

