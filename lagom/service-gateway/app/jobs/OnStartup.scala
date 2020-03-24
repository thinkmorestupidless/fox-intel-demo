package jobs

import javax.inject.Inject
import org.slf4j.LoggerFactory

class OnStartup @Inject() () {

  val log = LoggerFactory.getLogger(getClass)

  log.info("starting up")
}
