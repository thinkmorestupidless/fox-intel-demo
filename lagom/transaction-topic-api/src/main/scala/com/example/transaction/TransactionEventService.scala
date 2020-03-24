package com.example.transaction

import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import play.api.libs.json.{Format, Json}

object TransactionEventService {
  val TOPIC_NAME = "transactions"
}

trait TransactionEventService extends Service {

  def transactionsReceived(): Topic[TransactionReceived]

  override final def descriptor: Descriptor = {
    import Service._

    named("transaction-events")
      .withTopics(
        topic(TransactionEventService.TOPIC_NAME, transactionsReceived _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[TransactionReceived](_.header.id.toString)))
      .withAutoAcl(true)
  }
}

case class MailHeader(id: String, from: String, to: String, timestamp: Long)

object MailHeader {
  implicit val format: Format[MailHeader] = Json.format
}

case class TransactionReceived(header: MailHeader, domain: String)

object TransactionReceived {
  implicit val format: Format[TransactionReceived] = Json.format
}
