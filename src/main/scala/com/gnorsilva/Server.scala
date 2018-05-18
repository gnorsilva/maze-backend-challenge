package com.gnorsilva

import akka.actor.{ActorSystem, Props}
import akka.io.Tcp

import scala.concurrent.duration.{FiniteDuration, _}

object Server {

  case class Config(eventWindowSize: Int, eventWindowDuration: FiniteDuration)

  val system = ActorSystem()

  def start(config: Config = Config(eventWindowSize = 100000, eventWindowDuration = 10 seconds)) = {
    val tcpManager = Tcp.get(system).manager
    val clientConnection = system.actorOf(Props(classOf[ClientConnectionManager], tcpManager))
    val eventProcessor = system.actorOf(Props(classOf[EventProcessor], config, clientConnection))
    system.actorOf(Props(classOf[EventConnectionManager], tcpManager, eventProcessor))
  }

  def stop = {
    system.terminate()
  }

}
