package com.gnorsilva

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.{IO, Tcp}
import akka.io.Tcp._

class EventConnectionManager(clientManager: ActorRef) extends Actor with ActorLogging {

  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 9090))

  override def receive: Receive = {
    case Bound(local) =>
      log.info(s"Server started on $local")
    case Connected(remote, local) =>
      log.info(s"New connnection: $local -> $remote")
      sender() ! Register(self)
    case Received(data) =>
      data.utf8String.lines.foreach(line => {
        val i = line.lastIndexOf ("|")
        val id = line.substring(i + 1).toInt
        clientManager ! ClientEvent(id, line)
      })
  }
}
