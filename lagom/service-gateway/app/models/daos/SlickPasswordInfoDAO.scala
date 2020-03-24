package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class SlickPasswordInfoDAO @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
  extends DelegableAuthInfoDAO[PasswordInfo]
  with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  case class PasswordLoginInfo(
      providerId: String,
      providerKey: String,
      hasher: String,
      password: String,
      salt: Option[String]
  )

  private class PasswordInfoTable(tag: Tag) extends Table[PasswordLoginInfo](tag, "login_info_password") {

    def providerId = column[String]("provider_id")
    def providerKey = column[String]("provider_key")
    def hasher = column[String]("hasher")
    def password = column[String]("password")
    def salt = column[Option[String]]("salt")
    def pk = primaryKey("pk_a", (providerId, providerKey))

    def * = (providerId, providerKey, hasher, password, salt) <> (PasswordLoginInfo.tupled, PasswordLoginInfo.unapply)
  }

  private val PasswordInfos = TableQuery[PasswordInfoTable]

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    db.run(
      PasswordInfos
        .filter(_.providerKey === loginInfo.providerKey)
        .filter(_.providerId === loginInfo.providerID)
        .result
        .headOption
        .map {
          case Some(o) ⇒ Some(PasswordInfo(o.hasher, o.password, o.salt))
          case None    ⇒ None
        }
    )

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val passwordLoginInfo =
      PasswordLoginInfo(loginInfo.providerID, loginInfo.providerKey, authInfo.hasher, authInfo.password, authInfo.salt)
    db.run(PasswordInfos += passwordLoginInfo)
      .map { _ ⇒
        authInfo
      }
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val q = for { c ← PasswordInfos if c.providerKey === loginInfo.providerKey } yield (c.hasher, c.password, c.salt)
    q.update((authInfo.hasher, authInfo.password, authInfo.salt))
    db.run(q.result.head.map { _ ⇒
      authInfo
    })
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    find(loginInfo).flatMap {
      case Some(_) ⇒ update(loginInfo, authInfo)
      case None    ⇒ add(loginInfo, authInfo)
    }

  override def remove(loginInfo: LoginInfo): Future[Unit] =
    db.run(PasswordInfos.filter(_.providerKey === loginInfo.providerKey).delete).map { _ ⇒
      ()
    }

  override val classTag = ClassTag(classOf[PasswordInfo])
}
