package com.example.cloudflow

import akka.stream.scaladsl
import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import com.example.data.MailHeader
import com.example.data.Newsletter

class NewsletterDetector extends AkkaStreamlet {

  val in = AvroInlet[MailHeader]("in")
  val out = AvroOutlet[Newsletter]("out")

  val shape = StreamletShape(in, out)

  final override def createLogic() = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): scaladsl.RunnableGraph[_] =
      sourceWithOffsetContext(in)
      .filter(isNewsletter)
      .map { header =>
        Newsletter(header)
      }
      .to(committableSink(out))

    def isNewsletter(header: MailHeader) =
      domain(header.from) == "newsletter.com"

    def domain(address: String) =
      address.split("@")(1)
  }
}

