package com.gnorsilva

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging}
import akka.io.{IO, Tcp}
import akka.io.Tcp._

class ClientConnectionManager extends Actor with ActorLogging {

  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 9099))

  override def receive: Receive = {
    case Bound(local) =>
      log.info(s"Server started on $local")
    case Connected(remote, local) =>
      log.info(s"New connnection: $local -> $remote")
      sender() ! Register(self)
    case Received(data) =>
  }
}
