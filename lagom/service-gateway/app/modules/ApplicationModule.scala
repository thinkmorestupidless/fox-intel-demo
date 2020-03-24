package modules

import com.google.inject.AbstractModule
import jobs.OnStartup
import net.codingwell.scalaguice.ScalaModule
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.AkkaGuiceSupport

class ApplicationModule extends AbstractModule with ScalaModule with AkkaGuiceSupport {

  val log = LoggerFactory.getLogger(getClass)

  override def configure(): Unit = {

    log.info("initialising application")

    bind[OnStartup].asEagerSingleton()
  }
}
