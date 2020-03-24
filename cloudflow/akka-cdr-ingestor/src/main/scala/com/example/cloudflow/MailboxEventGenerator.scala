package com.example.cloudflow

import akka.NotUsed
import akka.stream.scaladsl.{RunnableGraph, Source}
import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroOutlet
import com.example.data.SyncMailbox

import scala.concurrent.duration._

class MailboxEventGenerator extends AkkaStreamlet {
  import FakeStuff._

  val out = AvroOutlet[SyncMailbox]("out")

  def shape() = StreamletShape(out)

  override protected def createLogic() = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      Source.repeat(NotUsed)
        .throttle(100, 1 second)
        .map { _ =>
          val user = randomUser
          SyncMailbox(user.uuid, user.email, "", 0, System.currentTimeMillis())
        }
        .to(plainSink(out))
    }
  }
}
