package com.gnorsilva

import akka.actor.{ActorSystem, Props}

object Server {

  val system = ActorSystem()

  def start = {
    system.actorOf(Props(classOf[EventConnectionManager]))
  }

  def stop = {
    system.terminate()
  }

}
