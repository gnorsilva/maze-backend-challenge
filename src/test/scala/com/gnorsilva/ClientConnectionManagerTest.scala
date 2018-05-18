package com.gnorsilva


import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props}
import akka.io.Tcp.{Closed, Received, Write}
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}

import scala.concurrent.duration._

class ClientConnectionManagerTest extends TestKit(ActorSystem("ClientConnectionManagerTest")) with ImplicitSender
  with FreeSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "ClientConnectionManager" - {
    val stubIo: ActorRef = system.actorOf(TestActors.blackholeProps)
    val clientManager = system.actorOf(Props(classOf[ClientConnectionManager], stubIo))

    "sends messages to registered clients based on their Ids" in {
      clientManager ! Received(ByteString("123\r\n"))
      clientManager ! ClientEvents(Seq(ClientEvent(1, 123, "Hello World|123")))

      expectMsg(Write(ByteString("Hello World|123\r\n")))
    }

    "does not send messages to unregistered clients" in {
      clientManager ! Received(ByteString("123\r\n"))
      clientManager ! Closed
      clientManager ! ClientEvents(Seq(ClientEvent(1, 123, "Hello World|123")))

      expectNoMessage(100 millis)
    }

    "ignores non-id client messages" in {
      val failures = TestProbe()
      val props = Props(classOf[ClientConnectionManager], stubIo)
      val failureParent = system.actorOf(Props(new FailureWatchingActor(failures, props)))
      failureParent ! Received(ByteString("some random string"))
      failures.expectNoMessage(100 millis)
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
