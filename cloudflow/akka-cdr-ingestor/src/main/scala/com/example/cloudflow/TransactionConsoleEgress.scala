package com.example.cloudflow

import akka.stream.scaladsl.{RunnableGraph, Sink}
import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import com.example.data.TransactionWithContent

class TransactionConsoleEgress extends AkkaStreamlet {

  val in = AvroInlet[TransactionWithContent]("in")

  val shape = StreamletShape(in)

  override protected def createLogic() = new RunnableGraphStreamletLogic() {

    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(in)
        .map { tx =>
          System.out.println("Transaction! -> " + tx)
          tx.toString
        }
        .to(Sink.ignore)
    }
  }
}
