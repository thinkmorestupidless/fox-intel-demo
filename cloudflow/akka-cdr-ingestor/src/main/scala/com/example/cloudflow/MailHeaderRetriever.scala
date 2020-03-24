package com.example.cloudflow

import akka.stream.scaladsl.{RunnableGraph, Source}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import com.example.data.{MailHeader, SyncMailbox}

class MailHeaderRetriever extends AkkaStreamlet {

  val in = AvroInlet[SyncMailbox]("in")
  val out = AvroOutlet[MailHeader]("out")

  override def shape() = StreamletShape(in, out)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(in)
        .mapAsync(4) { cmd =>
          FakeStuff.asyncHeaders(cmd.userId, cmd.email)
        }
        .flatMapConcat { headers =>
          Source(headers)
        }
        .to(plainSink(out))
    }
  }
}
