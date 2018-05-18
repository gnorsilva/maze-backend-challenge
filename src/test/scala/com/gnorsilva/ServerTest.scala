package com.gnorsilva

import java.io.PrintStream
import java.net.{InetAddress, Socket}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, StreamConverters}
import com.gnorsilva.Server.Config
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
    Server.start(Config(eventWindowSize = 5, eventWindowDuration = 5 seconds))
    eventually {
      eventSocket = new Socket(InetAddress.getByName("localhost"), 9090)
      eventSocket.isConnected shouldBe true
      eventStream = new PrintStream(eventSocket.getOutputStream())
    }
  }

  override protected def afterAll(): Unit = {
    Server.stop
  }

  "Events are sent to the target connected client by event id order" in {
    val clientOne = setupClientConnection(1)
    val clientTwo = setupClientConnection(2)
    val clientThree = setupClientConnection(3)

    sendEvent("444|F|3|2\r\n333|F|1|3")
    sendEvent("555|F|3|1\r\n222|F|4|3\r\n111|F|6|2")
    sendEvent("777|F|8|1\r\n666|F|2|1\r\n999|F|7|1\r\n888|F|1|2")
    sendEvent("1000|F|9|3")

    clientReceivedContent(clientOne).shouldBe(
      "555|F|3|1\r\n666|F|2|1\r\n777|F|8|1\r\n999|F|7|1\r\n")

    clientReceivedContent(clientTwo).shouldBe(
      "111|F|6|2\r\n444|F|3|2\r\n888|F|1|2\r\n")

    clientReceivedContent(clientThree).shouldBe(
      "222|F|4|3\r\n333|F|1|3\r\n1000|F|9|3\r\n")
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

    val futureSequence = StreamConverters.fromInputStream(socket.getInputStream)
      .initialTimeout(timeout)
      .takeWithin(250 millis)
      .map(_.utf8String)
      .runWith(Sink.seq)

    Await.result(futureSequence, timeout).mkString("")
  }


}
