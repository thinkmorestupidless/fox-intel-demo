package models.daos

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import javax.inject.Inject
import less.stupid.user.api.{ CreateUser, User, UserService }
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits._

class LagomUserDAO @Inject() (userService: UserService) extends UserDAO {

  val log = LoggerFactory.getLogger(this.getClass)

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  override def find(loginInfo: LoginInfo): Future[Option[User]] = userService.findByLoginInfo(loginInfo.providerID, loginInfo.providerKey).invoke().transform {
    case Success(user) ⇒ Try(Some(user))
    case Failure(_)    ⇒ Try(None)
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  override def find(userID: UUID): Future[Option[User]] = userService.findById(userID).invoke().transform {
    case Success(user) ⇒ Try(Some(user))
    case Failure(_)    ⇒ Try(None)
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  override def save(user: User): Future[User] = userService.createUser.invoke(CreateUser(user.loginInfo))
}
