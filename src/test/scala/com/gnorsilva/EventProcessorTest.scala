package com.gnorsilva

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.gnorsilva.Server.Config
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers, OneInstancePerTest}

import scala.concurrent.duration._

class EventProcessorTest extends TestKit(ActorSystem("EventProcessorTest")) with ImplicitSender
  with FreeSpecLike with Matchers with BeforeAndAfterAll with OneInstancePerTest {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "EventProcessor" - {
    val config = Config(eventWindowSize = 3, eventWindowDuration = 5 seconds)
    val eventProcessor = system.actorOf(Props(classOf[EventProcessor], config, testActor))

    "Prepares ClientMessages for Following events" in {
      eventProcessor ! EventBatch(Seq(
        Follow(1, "1|F|60|4", 60, 4),
        Follow(2, "2|F|60|5", 60, 5),
        Follow(3, "3|F|60|6", 60, 6)
      ))

      expectMsg(ClientEvents(Seq(
        ClientMessage(4, "1|F|60|4"),
        ClientMessage(5, "2|F|60|5"),
        ClientMessage(6, "3|F|60|6")
      )))
    }

    "Prepares ClientMessages for Private messages" in {
      eventProcessor ! EventBatch(Seq(
        PrivateMessage(8, "8|P|60|4", 60, 4),
        PrivateMessage(9, "9|P|60|5", 60, 5),
        PrivateMessage(10, "10|P|60|6", 60, 6)
      ))

      expectMsg(ClientEvents(Seq(
        ClientMessage(4, "8|P|60|4"),
        ClientMessage(5, "9|P|60|5"),
        ClientMessage(6, "10|P|60|6")
      )))
    }

    "Prepares ClientMessages for status updates based on who is following whom at that time" in {
      eventProcessor ! EventBatch(Seq(
        Follow(1, "1|F|20|10", 20, 10),
        Follow(2, "2|F|30|10", 30, 10),
        Follow(3, "3|F|40|10", 40, 10),
        Unfollow(4, "4|U|30|10", 30, 10),
        StatusUpdate(5, "5|S|10", 10),
        Follow(6, "3|F|77|10", 77, 10)
      ))

      expectMsg(ClientEvents(Seq(
        ClientMessage(10, "1|F|20|10"),
        ClientMessage(10, "2|F|30|10"),
        ClientMessage(10, "3|F|40|10")
      )))

      expectMsg(ClientEvents(Seq(
        ClientMessage(20, "5|S|10"),
        ClientMessage(40, "5|S|10"),
        ClientMessage(10, "3|F|77|10")
      )))
    }

    "Prepares ClientBroadcasts for broadcast updates" in {
      eventProcessor ! EventBatch(Seq(
        Broadcast(1, "1|B"),
        Broadcast(2, "2|B"),
        Broadcast(3, "3|B")
      ))

      expectMsg(ClientEvents(Seq(
        ClientBroadcast("1|B"),
        ClientBroadcast("2|B"),
        ClientBroadcast("3|B")
      )))
    }

    "Sends ClientEvents based on the eventWindowSize and sorted for that window" in {
      eventProcessor ! EventBatch(Seq(
        Follow(4, "4|F|60|4", 60, 4),
        Follow(2, "2|F|60|5", 60, 5),
        Follow(3, "3|F|60|6", 60, 6),
        Follow(1, "1|F|60|4", 60, 4),
        Follow(6, "6|F|60|6", 60, 6),
        Follow(5, "5|F|60|5", 60, 5),
        Follow(9, "9|F|60|6", 60, 6),
        Follow(7, "7|F|60|4", 60, 4),
        Follow(8, "8|F|60|5", 60, 5)
      ))

      expectMsg(ClientEvents(Seq(
        ClientMessage(5, "2|F|60|5"),
        ClientMessage(6, "3|F|60|6"),
        ClientMessage(4, "4|F|60|4")
      )))

      expectMsg(ClientEvents(Seq(
        ClientMessage(4, "1|F|60|4"),
        ClientMessage(5, "5|F|60|5"),
        ClientMessage(6, "6|F|60|6")
      )))

      expectMsg(ClientEvents(Seq(
        ClientMessage(4, "7|F|60|4"),
        ClientMessage(5, "8|F|60|5"),
        ClientMessage(6, "9|F|60|6")
      )))
    }
  }
}
