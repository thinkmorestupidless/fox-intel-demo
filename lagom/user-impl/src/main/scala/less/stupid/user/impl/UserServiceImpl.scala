package less.stupid.user.impl

import java.util.UUID

import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.mohiva.play.silhouette.api.LoginInfo
import less.stupid.user.api
import less.stupid.user.api.UserService
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class UserServiceImpl(registry: PersistentEntityRegistry, repository: UserRepository)(implicit ec: ExecutionContext, mat: Materializer) extends UserService {

  val log = LoggerFactory.getLogger(this.getClass)

  override def createUser = ServiceCall { createUser ⇒
    val userId = UUID.randomUUID()
    refFor(userId).ask(CreateUser(createUser.loginInfo, None, None, None, None, None)).map { _ ⇒
      api.User(userId, createUser.loginInfo, None, None, None, None, None)
    }
  }

  def findByLoginInfo(providerId: String, providerKey: String) = ServiceCall { _ ⇒
    repository.find(LoginInfo(providerId, providerKey))
  }

  def findById(id: UUID) = ServiceCall { _ ⇒
    repository.find(id)
  }

  private def refFor(userId: UUID) = registry.refFor[UserEntity](userId.toString)
}
