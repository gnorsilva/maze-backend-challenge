package com.gnorsilva

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.collection.mutable

class ClientConnectionManager extends Actor with ActorLogging {

  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 9099))

  val connections: mutable.HashMap[Int, ActorRef] = mutable.HashMap()

  override def receive: Receive = {
    case Bound(local) =>
      log.info(s"Server started on $local")
    case Connected(remote, local) =>
      log.info(s"New connnection: $local -> $remote")
      sender() ! Register(self)
    case Received(data) =>
      val id = data.utf8String.stripLineEnd.trim.toInt
      connections += (id -> sender)
    case ClientEvent(id, event) =>
      connections.get(id).foreach(_ ! Write(ByteString(s"$event\r\n")))
  }
}

case class ClientEvent(clientId: Int, event: String)
