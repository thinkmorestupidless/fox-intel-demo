package com.example.mailbox.impl

import java.sql.Connection

import com.lightbend.lagom.scaladsl.persistence.jdbc.{JdbcReadSide, JdbcSession}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import org.slf4j.LoggerFactory

class MailboxEventProcessor(readSide: JdbcReadSide) extends ReadSideProcessor[MailboxEvent] {

  val log = LoggerFactory.getLogger(this.getClass)

  val createTableSql =
    s"""
      CREATE TABLE IF NOT EXISTS newsletters (
        domain VARCHAR(255) NOT NULL,
        count VARCHAR(255) NOT NULL,
        PRIMARY KEY (domain)
      );
     """

  val buildTables: Connection ⇒ Unit = { connection ⇒
    JdbcSession.tryWith(connection.createStatement()) {
      _.executeUpdate(createTableSql)
    }
  }

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[MailboxEvent] =
    readSide.builder[MailboxEvent]("mailbox-offset")
    .setGlobalPrepare(buildTables)
    .setEventHandler(handleMailboxStatusChanged)
    .setEventHandler(handleNewsletterAdded)
    .build()

  val handleMailboxStatusChanged: (Connection, EventStreamElement[MailboxStatusChanged]) => Unit =
    (_, eventElement) => {
      log.info(s"(${eventElement.entityId}) -> ${eventElement.event}")
    }

  val handleNewsletterAdded: (Connection, EventStreamElement[NewsletterAddedToMailbox]) => Unit =
    (_, eventElement) => {
      log.info(s"(${eventElement.entityId}) -> ${eventElement.event}")
    }

  override def aggregateTags = Set(MailboxEvent.Tag)
}
