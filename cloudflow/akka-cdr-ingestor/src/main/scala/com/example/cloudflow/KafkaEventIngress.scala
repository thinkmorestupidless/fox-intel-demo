package com.example.cloudflow

import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.RunnableGraph
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.avro.AvroOutlet
import cloudflow.streamlets.{StreamletShape, StringConfigParameter}
import com.example.data.SyncMailbox
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaEventIngress extends AkkaStreamlet {

  val out = AvroOutlet[SyncMailbox]("out")

  override def shape() = StreamletShape(out)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {

    val bootstrapServers = streamletConfig.getString(bootstrapServersParameter.getKey)
    val groupId = streamletConfig.getString(groupIdParameter.getKey)
    val topic = streamletConfig.getString(topicParameter.getKey)

    val consumerSettings = ConsumerSettings.create(system, new StringDeserializer(), new StringDeserializer())
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)

    override def runnableGraph(): RunnableGraph[_] = {
      Consumer.sourceWithOffsetContext(consumerSettings, Subscriptions.topics(topic))
        .map { msg =>
          SyncMailbox("", "", "", 0, 0)
        }
        .to(committableSink(out))
    }
  }

  val bootstrapServersParameter = StringConfigParameter(
    "bootstrap-servers",
    "Provide the Kafka bootstrap-servers to connect to"
  )

  val topicParameter = StringConfigParameter(
    "topic",
    "Provide the Kafka topic to read from",
    Some("mailbox")
  )

  val groupIdParameter = StringConfigParameter(
    "group-id",
    "Provide the Kafka consumer group that this streamlet is part of",
    Some("group-1")
  )

  override def configParameters = Vector(bootstrapServersParameter, topicParameter, groupIdParameter)
}
