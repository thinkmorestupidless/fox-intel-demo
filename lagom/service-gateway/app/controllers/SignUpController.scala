package controllers

import java.util.UUID

import com.mohiva.play.silhouette.api.{ LoginEvent, LoginInfo, SignUpEvent, Silhouette }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignUpForm
import javax.inject.Inject
import less.stupid.user.api.User
import models.services.UserService
import org.slf4j.LoggerFactory
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.json.Json
import play.api.mvc.{ AbstractController, ControllerComponents }
import utils.auth.DefaultEnv

import scala.concurrent.{ ExecutionContext, Future }

/**
 * The `Sign Up` controller.
 *
 * @param components The ControllerComponents.
 * @param silhouette The Silhouette stack.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param avatarService The avatar service implementation.
 * @param passwordHasher The password hasher implementation.
 */
class SignUpController @Inject() (
    components: ControllerComponents,
    silhouette: Silhouette[DefaultEnv],
    userService: UserService,
    authInfoRepository: AuthInfoRepository,
    avatarService: AvatarService,
    passwordHasher: PasswordHasher
)(implicit ec: ExecutionContext)
  extends AbstractController(components)
  with I18nSupport {

  val log = LoggerFactory.getLogger(this.getClass)

  /**
   * Handles the submitted JSON data.
   *
   * @return The result to display.
   */
  def submit = Action.async(parse.json) { implicit request ⇒
    request.body
      .validate[SignUpForm.Data]
      .map { data ⇒
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(_) ⇒
            Future.successful(BadRequest(Json.obj("message" -> Messages("user.exists"))))
          case None ⇒
            val authInfo = passwordHasher.hash(data.password)
            val user = User(
              userID = UUID.randomUUID(),
              loginInfo = loginInfo,
              firstName = Some(data.firstName),
              lastName = Some(data.lastName),
              fullName = Some(data.firstName + " " + data.lastName),
              email = Some(data.email),
              avatarURL = None
            )
            for {
              avatar ← avatarService.retrieveURL(data.email)
              user ← userService.save(user.copy(avatarURL = avatar))
              _ ← authInfoRepository.add(loginInfo, authInfo)
              authenticator ← silhouette.env.authenticatorService.create(loginInfo)
              token ← silhouette.env.authenticatorService.init(authenticator)
            } yield {
              silhouette.env.eventBus.publish(SignUpEvent(user, request))
              silhouette.env.eventBus.publish(LoginEvent(user, request))
              Ok(Json.obj("token" -> token))
            }
        }
      }
      .recoverTotal {
        case _ ⇒
          Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.data"))))
      }
  }
}
