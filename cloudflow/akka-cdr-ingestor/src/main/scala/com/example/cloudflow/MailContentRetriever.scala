package com.example.cloudflow

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.RunnableGraph
import cloudflow.akkastream.AkkaStreamlet
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import com.example.data.{Transaction, TransactionWithContent}

class MailContentRetriever extends AkkaStreamlet {

  val in = AvroInlet[Transaction]("in")
  val out = AvroOutlet[TransactionWithContent]("out")

  val shape = StreamletShape(in, out)

  override protected def createLogic() = new RunnableGraphStreamletLogic() {
    override def runnableGraph(): RunnableGraph[_] = {
      sourceWithOffsetContext(in)
        .mapAsync(4) { tx =>
          val http = Http(context.system)
          val request = HttpRequest(uri = "https://baconipsum.com/api/?type=meat-and-filler")

          http.singleRequest(request)
            .flatMap { response =>
              Unmarshal(response.entity).to[String].map { content =>
                TransactionWithContent(tx.header, content)
              }
            }
        }
        .to(committableSink(out))
    }
  }
}
