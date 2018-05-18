package com.gnorsilva

import akka.actor.{Actor, ActorRef}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.gnorsilva.Server.Config

import scala.collection.mutable

class EventProcessor(config: Config, clientManager: ActorRef) extends Actor {

  private implicit val materializer = ActorMaterializer()

  private val queue = Source.queue[Event](Int.MaxValue, OverflowStrategy.backpressure)
    .groupedWithin(config.eventWindowSize, config.eventWindowDuration)
    .map(_.sorted)
    .map(processEvents)
    .to(Sink.actorRef(clientManager, ""))
    .run()

  override def receive: Receive = {
    case EventBatch(data) => data.foreach(queue.offer)
  }

  val followers: mutable.HashMap[Int, mutable.HashSet[Int]] = mutable.HashMap()

  def processEvents: Seq[Event] => ClientEvents = { seq =>

    val processed = seq.map {
      case Follow(id, message, from, to) =>
        followers.getOrElseUpdate(to, mutable.HashSet()) += from
        Seq(ClientMessage(to, message))

      case Unfollow(_, _, from, to) =>
        followers.get(to).foreach(set => set -= from)
        Seq()

      case PrivateMessage(id, message, _, to) =>
        Seq(ClientMessage(to, message))

      case StatusUpdate(id, message, from) =>
        followers.get(from) match {
          case Some(set) => set.map(followerId => ClientMessage(followerId, message)).toSeq
          case _ => Seq()
        }

      case Broadcast(_, message) =>
        Seq(ClientBroadcast(message))
    }

    ClientEvents(processed.flatten)
  }
}

