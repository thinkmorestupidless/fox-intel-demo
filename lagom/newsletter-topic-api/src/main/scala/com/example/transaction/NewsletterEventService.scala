package com.example.transaction

import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import play.api.libs.json.{Format, Json}

object NewsletterEventService {
  val TOPIC_NAME = "newsletters"
}

trait NewsletterEventService extends Service {

  def newslettersReceived(): Topic[NewsletterReceived]

  override final def descriptor: Descriptor = {
    import Service._

    named("newsletter-events")
      .withTopics(
        topic(NewsletterEventService.TOPIC_NAME, newslettersReceived _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[NewsletterReceived](_.header.id.toString)))
      .withAutoAcl(true)
  }
}

case class MailHeader(id: String, from: String, to: String, timestamp: Long)

object MailHeader {
  implicit val format: Format[MailHeader] = Json.format
}

case class NewsletterReceived(header: MailHeader, domain: String)

object NewsletterReceived {
  implicit val format: Format[NewsletterReceived] = Json.format
}
