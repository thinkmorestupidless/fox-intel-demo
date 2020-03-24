package com.example.mailbox.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.example.mailbox.api.MailboxService
import com.example.transaction.NewsletterEventService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.softwaremill.macwire._
import play.api.db.HikariCPComponents

class MailboxLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new MailboxApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new MailboxApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[MailboxService])
}

abstract class MailboxApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with JdbcPersistenceComponents
  with HikariCPComponents
  with LagomKafkaComponents
  with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[MailboxService](wire[MailboxServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = MailboxSerializerRegistry

  readSide.register(wire[MailboxEventProcessor])

  wire[NewsletterEventSubscriber]

  lazy val newsletterService = serviceClient.implement[NewsletterEventService]

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(MailboxState.typeKey)(
      entityContext => MailboxBehavior.create(entityContext)))

}
