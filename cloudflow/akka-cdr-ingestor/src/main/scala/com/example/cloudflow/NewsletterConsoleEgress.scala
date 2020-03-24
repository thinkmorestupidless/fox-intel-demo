package com.example.cloudflow

import akka.stream.scaladsl.{RunnableGraph, Sink}
import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import com.example.data.Newsletter

class NewsletterConsoleEgress extends AkkaStreamlet {

  val in = AvroInlet[Newsletter]("in")

  val shape = StreamletShape(in)

  override protected def createLogic() = new RunnableGraphStreamletLogic() {

    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(in)
        .map { nl =>
          System.out.println("Newsletter! -> " + nl)
          nl.toString
        }
        .to(Sink.ignore)
    }
  }
}
