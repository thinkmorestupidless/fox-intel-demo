package com.example.cloudflow

import akka.stream.scaladsl
import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import com.example.data.MailHeader
import com.example.data.Transaction

class TransactionDetector extends AkkaStreamlet {

  val in = AvroInlet[MailHeader]("in")
  val out = AvroOutlet[Transaction]("out")

  val shape = StreamletShape(in, out)

  final override def createLogic() = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): scaladsl.RunnableGraph[_] =
      sourceWithOffsetContext(in)
      .filter(isTransaction)
      .map { header =>
        Transaction(header)
      }
      .to(committableSink(out))

    def isTransaction(header: MailHeader) =
      domain(header.from) == "transaction.com"

    def domain(address: String) =
      address.split("@")(1)
  }
}
