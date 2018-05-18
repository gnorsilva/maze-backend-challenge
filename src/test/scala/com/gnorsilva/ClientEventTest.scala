package com.gnorsilva

import org.scalatest.{FreeSpec, Matchers}

class ClientEventTest extends FreeSpec with Matchers {

  "ClientEvent can be sorted by event Id" in {
    val seq = Seq(
      ClientEvent(3, 38, "some message"),
      ClientEvent(2, 30, "some message 2"),
      ClientEvent(4, 34, "some message 2"),
      ClientEvent(1, 34, "some message 3")
    )

    seq.sorted.shouldBe(Seq(
      ClientEvent(1, 34, "some message 3"),
      ClientEvent(2, 30, "some message 2"),
      ClientEvent(3, 38, "some message"),
      ClientEvent(4, 34, "some message 2")
    ))
  }

}
