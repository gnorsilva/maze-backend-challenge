package com.gnorsilva

import org.scalatest.{FreeSpec, Matchers}

class EventsTest extends FreeSpec with Matchers {

  "Events are sorted by event Id" in {
    val seq = Seq(
      Follow(666, "message", 60, 50),
      Unfollow(1, "message", 12, 9),
      Broadcast(542532, "message"),
      PrivateMessage(43, "message", 32, 56),
      StatusUpdates(634, "message", 32)
    )

    seq.sorted.shouldBe(Seq(
      Unfollow(1, "message", 12, 9),
      PrivateMessage(43, "message", 32, 56),
      StatusUpdates(634, "message", 32),
      Follow(666, "message", 60, 50),
      Broadcast(542532, "message")
    ))
  }

}
