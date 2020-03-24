package less.stupid.user.api

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{ Service, ServiceCall }
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import play.api.libs.json.{ Format, Json }

trait UserService extends Service {
  def createUser: ServiceCall[CreateUser, User]
  def findByLoginInfo(providerId: String, providerKey: String): ServiceCall[NotUsed, User]
  def findById(id: UUID): ServiceCall[NotUsed, User]

  def descriptor = {
    import Service._
    named("user-service").withCalls(
      pathCall("/api/user", createUser),
      pathCall("/api/user/:id", findById _),
      pathCall("/api/user/:id/:key", findByLoginInfo _)
    )
  }
}

case class User(
    userID: UUID,
    loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    fullName: Option[String],
    email: Option[String],
    avatarURL: Option[String]
) extends Identity

object User {
  implicit val format: Format[User] = Json.format

  def apply(userId: UUID, loginInfo: LoginInfo): User =
    User(userId, loginInfo, None, None, None, None, None)
}

case class CreateUser(loginInfo: LoginInfo)

object CreateUser {
  implicit val format: Format[CreateUser] = Json.format
}
