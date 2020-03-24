package models.daos

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import javax.inject.Inject
import less.stupid.user.api.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class SlickUserDAO @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
  extends UserDAO
  with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  case class UserRecord(
      id: UUID,
      firstName: Option[String],
      lastName: Option[String],
      fullName: Option[String],
      email: Option[String],
      avatarURL: Option[String])

  class UserRecordTable(tag: Tag) extends Table[UserRecord](tag, "users") {
    def id = column[UUID]("id", O.PrimaryKey)
    def email = column[Option[String]]("email")
    def firstName = column[Option[String]]("first_name")
    def lastName = column[Option[String]]("last_name")
    def fullName = column[Option[String]]("full_name")
    def avatarURL = column[Option[String]]("avatar_url")

    override def * = (id, email, firstName, lastName, fullName, avatarURL) <> (UserRecord.tupled, UserRecord.unapply)
  }

  private val StoredUsers = TableQuery[UserRecordTable]

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  override def find(loginInfo: LoginInfo): Future[Option[User]] =
    db.run(StoredUsers.filter(_.email === loginInfo.providerKey).result.headOption.map {
      case Some(u) ⇒ Some(User(u.id, loginInfo, u.firstName, u.lastName, u.fullName, u.email, u.avatarURL))
      case None    ⇒ None
    })

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  override def find(userID: UUID): Future[Option[User]] =
    db.run(StoredUsers.filter(_.id === userID).result.headOption.map {
      case Some(u) ⇒ Some(User(u.id, LoginInfo("", ""), u.firstName, u.lastName, u.fullName, u.email, u.avatarURL))
      case None    ⇒ None
    })

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  override def save(user: User): Future[User] = {
    val userRecord = UserRecord(user.userID, user.email, user.firstName, user.lastName, user.fullName, user.avatarURL)
    db.run(StoredUsers += userRecord)
      .map { _ ⇒
        user
      }
  }
}
