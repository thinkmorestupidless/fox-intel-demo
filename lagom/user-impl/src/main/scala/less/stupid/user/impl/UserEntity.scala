package less.stupid.user.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEvent, AggregateEventTag, PersistentEntity }
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{ JsonSerializer, JsonSerializerRegistry }
import com.mohiva.play.silhouette.api.LoginInfo
import less.stupid.user.impl.JsonFormats._
import org.slf4j.LoggerFactory
import play.api.libs.json.{ Format, Json }

class UserEntity extends PersistentEntity {
  override type Command = UserCommand
  override type Event = UserEvent
  override type State = Option[User]
  override def initialState = None

  val log = LoggerFactory.getLogger(this.getClass)

  override def behavior: Behavior = {
    case Some(_) ⇒
      Actions().onReadOnlyCommand[GetUser.type, Option[User]] {
        case (GetUser, ctx, state) ⇒ ctx.reply(state)
      }.onReadOnlyCommand[CreateUser, Done] {
        case (CreateUser(_, _, _, _, _, _), ctx, _) ⇒ ctx.invalidCommand("User already exists")
      }
    case None ⇒
      Actions().onReadOnlyCommand[GetUser.type, Option[User]] {
        case (GetUser, ctx, state) ⇒ ctx.reply(state)
      }.onCommand[CreateUser, Done] {
        case (CreateUser(loginInfo, firstName, lastName, fullName, email, avatarURL), ctx, _) ⇒
          ctx.thenPersist(UserCreated(loginInfo, firstName, lastName, fullName, email, avatarURL))(_ ⇒ ctx.reply(Done))
      }.onEvent {
        case (UserCreated(loginInfo, firstName, lastName, fullName, email, avatarURL), _) ⇒ Some(User(loginInfo, firstName, lastName, fullName, email, avatarURL))
      }
  }
}

case class User(
    loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    fullName: Option[String],
    email: Option[String],
    avatarURL: Option[String]
)

object User {
  implicit val format: Format[User] = Json.format
}

sealed trait UserEvent extends AggregateEvent[UserEvent] {
  override def aggregateTag = UserEvent.Tag
}

object UserEvent {
  def Tag = AggregateEventTag.sharded[UserEvent](4)
}

case class UserCreated(
    loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    fullName: Option[String],
    email: Option[String],
    avatarURL: Option[String]) extends UserEvent

object UserCreated {
  implicit val format: Format[UserCreated] = Json.format
}

sealed trait UserCommand

case class CreateUser(
    loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    fullName: Option[String],
    email: Option[String],
    avatarURL: Option[String]
) extends UserCommand with ReplyType[Done]

object CreateUser {
  implicit val format: Format[CreateUser] = Json.format
}

case object GetUser extends UserCommand with ReplyType[Option[User]] {
  implicit val format: Format[GetUser.type] = singletonFormat(GetUser)
}

object UserSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[User],
    JsonSerializer[UserCreated],
    JsonSerializer[CreateUser],
    JsonSerializer[GetUser.type]
  )
}
