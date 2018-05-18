package com.gnorsilva

import akka.actor.{Actor, ActorRef}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.gnorsilva.Server.Config

class EventProcessor(config: Config, clientManager: ActorRef) extends Actor {

  private implicit val materializer = ActorMaterializer()

  private val queue = Source.queue[ClientEvent](Int.MaxValue, OverflowStrategy.backpressure)
    .groupedWithin(config.eventWindowSize, config.eventWindowDuration)
    .map(_.sorted)
    .map(ClientEvents(_))
    .to(Sink.actorRef(clientManager, ""))
    .run()

  override def receive: Receive = {
    case EventBatch(data) =>
      data.foreach(line => {
        val eventId = line.substring(0, line.indexOf("|")).toInt
        val clientId = line.substring(line.lastIndexOf("|") + 1).toInt
        queue.offer(ClientEvent(eventId, clientId, line))
      })
  }
}

case class EventBatch(lines: Iterator[String])

