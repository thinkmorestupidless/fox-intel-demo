package less.stupid.user.impl

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import less.stupid.user.api.UserService
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class UserApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with AhcWSComponents
  with JdbcPersistenceComponents
  with HikariCPComponents {

  override lazy val lagomServer = serverFor[UserService](wire[UserServiceImpl])
  override lazy val jsonSerializerRegistry = UserSerializerRegistry

  persistentEntityRegistry.register(wire[UserEntity])

  readSide.register(wire[UserEventProcessor])

  lazy val repository = wire[UserRepository]
}

class UserApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new UserApplication(context) with ConfigurationServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new UserApplication(context) with ConfigurationServiceLocatorComponents

  override def describeService = Some(readDescriptor[UserService])
}
