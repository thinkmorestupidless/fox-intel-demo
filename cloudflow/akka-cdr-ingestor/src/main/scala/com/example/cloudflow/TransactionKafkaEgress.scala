package com.example.cloudflow

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{RunnableGraph, Sink}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.{StreamletShape, StringConfigParameter}
import cloudflow.streamlets.avro.AvroInlet
import com.example.data.{Transaction, TransactionWithContent}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

class TransactionKafkaEgress extends AkkaStreamlet {

  val in = AvroInlet[TransactionWithContent]("in")

  val shape = StreamletShape(in)

  override protected def createLogic() = new RunnableGraphStreamletLogic() {

    val bootstrapServers = streamletConfig.getString(bootstrapServersParameter.getKey)
    val topic = streamletConfig.getString(topicParameter.getKey)

    val producerSettings =
      ProducerSettings(system, new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)

    override def runnableGraph(): RunnableGraph[_] = {
      plainSource(in)
        .map { tx =>
          System.out.println("Transaction! -> " + tx)
          tx.toString
        }
        .map(value => new ProducerRecord[String, String](topic, value))
        .to(Producer.plainSink(producerSettings))
    }
  }

  val bootstrapServersParameter = StringConfigParameter(
    "bootstrap-servers",
    "Provide the Kafka bootstrap-servers to connect to"
  )

  val topicParameter = StringConfigParameter(
    "topic",
    "Provide the Kafka topic to read from",
    Some("transactions")
  )

  override def configParameters = Vector(bootstrapServersParameter, topicParameter)
}
