package less.stupid.user.impl

import java.sql.Connection

import com.lightbend.lagom.scaladsl.persistence.jdbc.{ JdbcReadSide, JdbcSession }
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, EventStreamElement, ReadSideProcessor }

import scala.concurrent.ExecutionContext

class UserEventProcessor(readSide: JdbcReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[UserEvent] {

  val createTableSql =
    s"""
       CREATE TABLE IF NOT EXISTS users (
        user_id VARCHAR(255) NOT NULL,
        provider_key VARCHAR(255) NOT NULL,
        provider_id VARCHAR(255) NOT NULL,
        PRIMARY KEY (provider_id, provider_key)
       );"""

  val buildTables: Connection ⇒ Unit = { connection ⇒
    JdbcSession.tryWith(connection.createStatement()) {
      _.executeUpdate(createTableSql)
    }
  }

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[UserEvent] =
    readSide.builder[UserEvent]("user_offset")
      .setGlobalPrepare(buildTables)
      .setEventHandler(handleUserCreated)
      .build()

  val handleUserCreated: (Connection, EventStreamElement[UserCreated]) ⇒ Unit =
    (connection, eventElement) ⇒
      JdbcSession.tryWith(
        connection.prepareStatement(
          s"""
             INSERT INTO users (user_id, provider_key, provider_id)
             VALUES (?, ?, ?)
             """)
      ) { statement ⇒
          statement.setString(1, eventElement.entityId)
          statement.setString(2, eventElement.event.loginInfo.providerKey)
          statement.setString(3, eventElement.event.loginInfo.providerID)
          statement.executeUpdate()
        }

  override def aggregateTags: Set[AggregateEventTag[UserEvent]] = UserEvent.Tag.allTags
}
