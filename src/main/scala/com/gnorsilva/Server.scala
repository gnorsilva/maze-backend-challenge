package com.gnorsilva

import akka.actor.{ActorSystem, Props}
import akka.io.Tcp

object Server {

  val system = ActorSystem()

  def start = {
    val tcpManager = Tcp.get(system).manager
    val clientConnection = system.actorOf(Props(classOf[ClientConnectionManager], tcpManager))
    system.actorOf(Props(classOf[EventConnectionManager], tcpManager, clientConnection))
  }

  def stop = {
    system.terminate()
  }

}
