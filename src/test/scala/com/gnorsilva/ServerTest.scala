package com.gnorsilva

import java.net.{InetAddress, Socket}

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

class ServerTest extends FreeSpec with BeforeAndAfterAll with Eventually with Matchers with IntegrationPatience {

  override protected def beforeAll(): Unit = {
    Server.start
  }

  override protected def afterAll(): Unit = {
    Server.stop
  }

  "Server should open port 9090 for event stream connection" in {
    eventually {
      val eventSocket = new Socket(InetAddress.getByName("localhost"), 9090)
      eventSocket.isConnected shouldBe true
    }
  }

  "Server should open port 9099 for client connections" in {
    eventually {
      val clientOne = new Socket(InetAddress.getByName("localhost"), 9099)
      clientOne.isConnected shouldBe true
      val clientTwo = new Socket(InetAddress.getByName("localhost"), 9099)
      clientTwo.isConnected shouldBe true
    }
  }
}
