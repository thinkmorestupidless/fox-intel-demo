package less.stupid.user.impl

import java.sql.{ Connection, ResultSet }
import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcSession
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcSession.tryWith
import com.mohiva.play.silhouette.api.LoginInfo
import less.stupid.user.api

import scala.concurrent.{ ExecutionContext, Future }

class UserRepository(session: JdbcSession)(implicit ec: ExecutionContext) {

  def findByUserIdQuery(connection: Connection, userId: UUID): ResultSet = {
    val statement = connection.prepareStatement(s"SELECT * FROM users WHERE user_id = ?")
    statement.setString(1, userId.toString)
    statement.executeQuery()
  }

  def findByLoginInfoQuery(connection: Connection, loginInfo: LoginInfo): ResultSet = {
    val statement = connection.prepareStatement(s"SELECT * FROM users WHERE provider_id = ? AND provider_key = ?")
    statement.setString(1, loginInfo.providerID)
    statement.setString(2, loginInfo.providerKey)
    statement.executeQuery()
  }

  def convertUser(rs: ResultSet): api.User =
    api.User(
      UUID.fromString(rs.getString("user_id")),
      LoginInfo(rs.getString("provider_id"), rs.getString("provider_key"))
    )

  def find(userId: UUID): Future[api.User] = {
    session.withConnection { connection ⇒
      tryWith(findByUserIdQuery(connection, userId)) { rs ⇒
        if (rs.next()) {
          convertUser(rs)
        } else {
          throw new IllegalStateException(s"No User exists with userId $userId")
        }
      }
    }
  }

  def find(loginInfo: LoginInfo): Future[api.User] = {
    session.withConnection { connection ⇒
      tryWith(findByLoginInfoQuery(connection, loginInfo)) { rs ⇒
        if (rs.next()) {
          convertUser(rs)
        } else {
          throw new IllegalStateException(s"No User exists with loginInfo $loginInfo")
        }
      }
    }
  }
}
