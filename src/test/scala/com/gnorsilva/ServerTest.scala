package com.gnorsilva

import java.io.PrintStream
import java.net.{InetAddress, Socket}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, StreamConverters}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._


class ServerTest extends FreeSpec with BeforeAndAfterAll with Eventually with Matchers with IntegrationPatience {

  implicit val system: ActorSystem = ActorSystem("TestEnvironment")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  var eventSocket: Socket = _
  var eventStream: PrintStream = _

  override protected def beforeAll(): Unit = {
    Server.start
    eventually {
      eventSocket = new Socket(InetAddress.getByName("localhost"), 9090)
      eventSocket.isConnected shouldBe true
      eventStream = new PrintStream(eventSocket.getOutputStream())
    }
  }

  override protected def afterAll(): Unit = {
    Server.stop
  }

  "Events are sent to the target connected client" in {
    val clientOne = setupClientConnection(1)
    val clientTwo = setupClientConnection(2)

    sendEvent("834|F|9|2")
    sendEvent("555|F|2|1")

    clientReceivedContent(clientOne).shouldBe("555|F|2|1\r\n")
    clientReceivedContent(clientTwo).shouldBe("834|F|9|2\r\n")
  }

  def setupClientConnection(id: Int): Socket = {
    val client = new Socket(InetAddress.getByName("localhost"), 9099)
    eventually {
      client.isConnected shouldBe true
    }
    val stream = new PrintStream(client.getOutputStream)
    stream.print(s"$id\r\n")
    stream.flush()
    client
  }

  def sendEvent(content: String) = {
    eventStream.print(s"$content\r\n")
    eventStream.flush()
  }

  def clientReceivedContent(socket: Socket): String = {
    val timeout = 5 seconds

    val eventualString = StreamConverters.fromInputStream(socket.getInputStream)
      .initialTimeout(timeout)
      .map(_.utf8String)
      .runWith(Sink.head)

    Await.result(eventualString, timeout)
  }

}
