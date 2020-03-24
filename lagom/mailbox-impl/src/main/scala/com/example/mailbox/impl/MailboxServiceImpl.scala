package com.example.mailbox.impl

import java.util.UUID

import akka.{Done, NotUsed}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.example.mailbox.api
import com.example.mailbox.api.MailboxService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Implementation of the MailboxService.
 */
class MailboxServiceImpl(
  clusterSharding: ClusterSharding,
  persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext)
  extends MailboxService {

  val log = LoggerFactory.getLogger(this.getClass)

  /**
   * Looks up the entity for the given ID.
   */
  private def entityRef(id: String): EntityRef[MailboxCommand] =
    clusterSharding.entityRefFor(MailboxState.typeKey, id)

  implicit val timeout = Timeout(5.seconds)

  override def syncMailbox(id: String): ServiceCall[NotUsed, Done] = ServiceCall {
    _ =>
      val ref = entityRef(id)

      ref.ask[Done](replyTo => Sync(id, replyTo))
  }

  override def mailboxStatus(id: String) = ServiceCall { request =>
    // Look up the sharded entity (aka the aggregate instance) for the given ID.
    val ref = entityRef(id)

    // Tell the aggregate to use the greeting message specified.
    ref
      .ask[String](
        replyTo => Status(id, replyTo))
      .map {
        case x: String => x
        case _ => throw BadRequest("I have no idea what i'm doing!")
      }
  }

  override def mailboxTopic(): Topic[api.MailboxEvent] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(MailboxEvent.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(mailboxEvent: EventStreamElement[MailboxEvent]): api.MailboxEvent = {
    mailboxEvent.event match {
      case MailboxStatusChanged(from, to) =>
        log.info(s"publishing public mailbox status change (${mailboxEvent.entityId}) $from -> $to")
        api.MailboxStatusChanged(mailboxEvent.entityId, from, to)
    }
  }
}
