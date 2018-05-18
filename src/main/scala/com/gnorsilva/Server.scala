package com.gnorsilva

import akka.actor.{ActorSystem, Props}

object Server {

  val system = ActorSystem()

  def start = {
    val clientConnection = system.actorOf(Props(classOf[ClientConnectionManager]))
    system.actorOf(Props(classOf[EventConnectionManager], clientConnection))
  }

  def stop = {
    system.terminate()
  }

}
