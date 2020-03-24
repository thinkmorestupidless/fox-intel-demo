package com.example.cloudflow

import akka.stream.scaladsl.RunnableGraph
import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.util.scaladsl.Merger
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import com.example.data.SyncMailbox

class MergeMailboxEvents extends AkkaStreamlet {

  val in0 = AvroInlet[SyncMailbox]("in-0")
  val in1 = AvroInlet[SyncMailbox]("in-1")
  val out = AvroOutlet[SyncMailbox]("out")

  override def shape(): StreamletShape = StreamletShape.withInlets(in0, in1).withOutlets(out)

  override protected def createLogic() = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      Merger.source(in0, in1)
        .to(committableSink(out))
    }
  }
}
