package com.gnorsilva

import akka.actor.{ActorSystem, Props}
import akka.io.Tcp
import com.gnorsilva.Server.Config

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object Start extends App {
  Server.start(Config(eventWindowSize = 1000000, eventWindowDuration = 10 seconds))
}

object Server {

  case class Config(eventWindowSize: Int, eventWindowDuration: FiniteDuration)

  val system = ActorSystem()

  def start(config: Config) = {
    val tcpManager = Tcp.get(system).manager
    val clientConnection = system.actorOf(Props(classOf[ClientConnectionManager], tcpManager))
    val eventProcessor = system.actorOf(Props(classOf[EventProcessor], config, clientConnection))
    system.actorOf(Props(classOf[EventConnectionManager], tcpManager, eventProcessor))
  }

  def stop = {
    system.terminate()
  }

}
