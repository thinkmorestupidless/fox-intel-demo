package com.example.mailbox.impl

import play.api.libs.json.Json
import play.api.libs.json.Format
import java.time.LocalDateTime
import java.util.UUID

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import com.example.transaction.NewsletterReceived
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import play.api.libs.json._

import scala.collection.immutable.Seq

/**
 * This provides an event sourced behavior. It has a state, [[MailboxState]], which
 * stores what the greeting should be (eg, "Hello").
 *
 * Event sourced entities are interacted with by sending them commands. This
 * aggregate supports two commands, a [[UseGreetingMessage]] command, which is
 * used to change the greeting, and a [[Hello]] command, which is a read
 * only command which returns a greeting to the name specified by the command.
 *
 * Commands get translated to events, and it's the events that get persisted.
 * Each event will have an event handler registered for it, and an
 * event handler simply applies an event to the current state. This will be done
 * when the event is first created, and it will also be done when the aggregate is
 * loaded from the database - each event will be replayed to recreate the state
 * of the aggregate.
 *
 * This aggregate defines one event, the [[GreetingMessageChanged]] event,
 * which is emitted when a [[UseGreetingMessage]] command is received.
 */
object MailboxBehavior {

  /**
   * Given a sharding [[EntityContext]] this function produces an Akka [[Behavior]] for the aggregate.
   */
  def create(entityContext: EntityContext[MailboxCommand]): Behavior[MailboxCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        // Using Akka Persistence Typed in Lagom requires tagging your events
        // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
        // can locate and follow the event streams.
        AkkaTaggerAdapter.fromLagom(entityContext, MailboxEvent.Tag))

  }
  /*
   * This method is extracted to write unit tests that are completely independendant to Akka Cluster.
   */
  private[impl] def create(persistenceId: PersistenceId) = EventSourcedBehavior
    .withEnforcedReplies[MailboxCommand, MailboxEvent, MailboxState](
      persistenceId = persistenceId,
      emptyState = MailboxState.initial,
      commandHandler = (cart, cmd) => cart.applyCommand(cmd),
      eventHandler = (cart, evt) => cart.applyEvent(evt))
}

/**
 * The current state of the Aggregate.
 */
case class MailboxState(status: String, domains: Set[String]) {
  def applyCommand(cmd: MailboxCommand): ReplyEffect[MailboxEvent, MailboxState] =
    cmd match {
      case x: Status => onStatus(x)
      case x: Sync => onSync(x)
      case x: AddNewsletterToMailbox => onNewsletter(x)
    }

  def applyEvent(evt: MailboxEvent): MailboxState =
    evt match {
      case MailboxStatusChanged(_, to) => updateStatus(to)
      case NewsletterAddedToMailbox(domain) => updateNewsletters(domain)
    }

  private def onStatus(cmd: Status): ReplyEffect[MailboxEvent, MailboxState] =
    Effect.reply(cmd.replyTo)(status)

  private def onSync(cmd: Sync): ReplyEffect[MailboxEvent, MailboxState] =
    Effect
    .persist(MailboxStatusChanged(status, "syncing"))
    .thenReply(cmd.replyTo) { _ =>
      Done
    }

  private def onNewsletter(cmd: AddNewsletterToMailbox): ReplyEffect[MailboxEvent, MailboxState] =
    Effect
    .persist(NewsletterAddedToMailbox(cmd.newsletter.domain))
    .thenReply(cmd.replyTo) { _ =>
      Done
    }

  private def updateStatus(status: String) =
    copy(status)

  private def updateNewsletters(domain: String) =
    copy(status, domains + (domain))
}

object MailboxState {

  /**
   * The initial state. This is used if there is no snapshotted state to be found.
   */
  def initial: MailboxState = MailboxState("Normal", Set.empty)

  /**
   * The [[EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
   * When sharding actors and distributing them across the cluster, each aggregate is
   * namespaced under a typekey that specifies a name and also the type of the commands
   * that sharded actor can receive.
   */
  val typeKey = EntityTypeKey[MailboxCommand]("MailboxAggregate")

  /**
   * Format for the hello state.
   *
   * Persisted entities get snapshotted every configured number of events. This
   * means the state gets stored to the database, so that when the aggregate gets
   * loaded, you don't need to replay all the events, just the ones since the
   * snapshot. Hence, a JSON format needs to be declared so that it can be
   * serialized and deserialized when storing to and from the database.
   */
  implicit val format: Format[MailboxState] = Json.format
}

/**
 * This interface defines all the events that the MailboxAggregate supports.
 */
sealed trait MailboxEvent extends AggregateEvent[MailboxEvent] {
  def aggregateTag: AggregateEventTag[MailboxEvent] = MailboxEvent.Tag
}

object MailboxEvent {
  val Tag: AggregateEventTag[MailboxEvent] = AggregateEventTag[MailboxEvent]
}

case class MailboxStatusChanged(from: String, to: String) extends MailboxEvent

object MailboxStatusChanged {
  implicit val format: Format[MailboxStatusChanged] = Json.format
}

case class NewsletterAddedToMailbox(domain: String) extends MailboxEvent

object NewsletterAddedToMailbox {
  implicit val format: Format[NewsletterAddedToMailbox] = Json.format
}

/**
 * This is a marker trait for commands.
 * We will serialize them using Akka's Jackson support that is able to deal with the replyTo field.
 * (see application.conf)
 */
trait MailboxCommandSerializable

/**
 * This interface defines all the commands that the MailboxAggregate supports.
 */
sealed trait MailboxCommand
  extends MailboxCommandSerializable

case class AddNewsletterToMailbox(newsletter: NewsletterReceived, replyTo: ActorRef[Done])
  extends MailboxCommand

case class Status(id: String, replyTo: ActorRef[String])
  extends MailboxCommand

case class Sync(id: String, replyTo: ActorRef[Done])
  extends MailboxCommand


/**
 * Akka serialization, used by both persistence and remoting, needs to have
 * serializers registered for every type serialized or deserialized. While it's
 * possible to use any serializer you want for Akka messages, out of the box
 * Lagom provides support for JSON, via this registry abstraction.
 *
 * The serializers are registered here, and then provided to Lagom in the
 * application loader.
 */
object MailboxSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    // state and events can use play-json, but commands should use jackson because of ActorRef[T] (see application.conf)
    JsonSerializer[MailboxStatusChanged],
    JsonSerializer[NewsletterAddedToMailbox],
    JsonSerializer[MailboxState])
}
