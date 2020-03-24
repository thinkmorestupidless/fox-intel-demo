package com.example.mailbox.api

import java.util.UUID

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object MailboxService {
  val TOPIC_NAME = "mailbox"
}

/**
 * The mailbox service interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the MailboxService.
 */
trait MailboxService extends Service {

  def syncMailbox(id: String): ServiceCall[NotUsed, Done]

  def mailboxStatus(id: String): ServiceCall[NotUsed, String]

  /**
   * This gets published to Kafka.
   */
  def mailboxTopic(): Topic[MailboxEvent]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("mailbox")
      .withCalls(
        pathCall("/api/sync/:id", syncMailbox _),
        pathCall("/api/mailbox/:id", mailboxStatus _)
      )
      .withTopics(
        topic(MailboxService.TOPIC_NAME, mailboxTopic _)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[MailboxEvent](_.id.toString)))
      .withAutoAcl(true)
    // @formatter:on
  }
}

sealed trait MailboxEvent {
  val id: String
}

object MailboxEvent {
  implicit val format: Format[MailboxEvent] = Json.format
}

case class MailboxStatusChanged(id: String, from: String, to: String) extends MailboxEvent

object MailboxStatusChanged {
  implicit val format: Format[MailboxStatusChanged] = Json.format
}
