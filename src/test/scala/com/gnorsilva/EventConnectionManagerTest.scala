package com.gnorsilva

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props}
import akka.io.Tcp.Received
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers, OneInstancePerTest}

import scala.concurrent.duration._

class EventConnectionManagerTest extends TestKit(ActorSystem("EventConnectionManagerTest")) with ImplicitSender
  with FreeSpecLike with Matchers with BeforeAndAfterAll with OneInstancePerTest {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "EventConnectionManager" - {
    val stubIo: ActorRef = system.actorOf(TestActors.blackholeProps)
    val eventManager = system.actorOf(Props(classOf[EventConnectionManager], stubIo, testActor))

    "parses Follow events" in {
      eventManager ! Received(ByteString("666|F|60|50\r\n"))

      expectMsg(EventBatch(Seq(Follow(666, "666|F|60|50", 60, 50))))
    }

    "parses Unfollow events" in {
      eventManager ! Received(ByteString("1|U|12|9\r\n"))

      expectMsg(EventBatch(Seq(Unfollow(1, "1|U|12|9", 12, 9))))
    }

    "parses Broadcast events" in {
      eventManager ! Received(ByteString("542532|B\r\n"))

      expectMsg(EventBatch(Seq(Broadcast(542532, "542532|B"))))
    }

    "parses Private message events" in {
      eventManager ! Received(ByteString("43|P|32|56\r\n"))

      expectMsg(EventBatch(Seq(PrivateMessage(43, "43|P|32|56", 32, 56))))
    }

    "parses Status updates events" in {
      eventManager ! Received(ByteString("634|S|32\r\n"))

      expectMsg(EventBatch(Seq(StatusUpdate(634, "634|S|32", 32))))
    }

    "ignores unknown or non-event messages" in {
      val failures = TestProbe()
      val props = Props(classOf[EventConnectionManager], stubIo, testActor)
      val failureParent = system.actorOf(Props(new FailureWatchingActor(failures, props)))
      failureParent ! Received(ByteString("999|Z|32|43\r\n"))
      failureParent ! Received(ByteString("some random string"))

      failures.expectNoMessage(100 millis)
      expectNoMessage(100 millis)
    }
  }

  class FailureWatchingActor(testProbe: TestProbe, props: Props) extends Actor {
    val child = context.actorOf(props, "child")

    override val supervisorStrategy = OneForOneStrategy() {
      case f =>
        testProbe.ref ! f
        Stop
    }

    def receive = {
      case msg => child forward msg
    }
  }

}
