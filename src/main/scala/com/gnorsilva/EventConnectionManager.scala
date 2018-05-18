package com.gnorsilva

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp._

class EventConnectionManager(tcpManager: ActorRef, eventProcessor: ActorRef) extends Actor with ActorLogging {

  tcpManager ! Bind(self, new InetSocketAddress("localhost", 9090))

  override def receive: Receive = {
    case Bound(local) =>
      log.info(s"Server started on $local")
    case Connected(remote, local) =>
      log.info(s"New connnection: $local -> $remote")
      sender() ! Register(self)
    case Received(data) =>
      eventProcessor ! EventBatch(data.utf8String.lines)
  }
}
