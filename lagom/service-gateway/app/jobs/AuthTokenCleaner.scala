package jobs

import akka.actor._
import com.mohiva.play.silhouette.api.util.Clock
import javax.inject.Inject
import jobs.AuthTokenCleaner.Clean
import models.services.AuthTokenService
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A job which cleanup invalid auth tokens.
 *
 * @param service The auth token service implementation.
 * @param clock The clock implementation.
 */
class AuthTokenCleaner @Inject() (
    service: AuthTokenService,
    clock: Clock
) extends Actor {

  val log = LoggerFactory.getLogger(getClass)

  /**
   * Process the received messages.
   */
  def receive: Receive = {
    case Clean ⇒
      val start = clock.now.getMillis
      val msg = new StringBuffer("\n")
      msg.append("=================================\n")
      msg.append("Start to cleanup auth tokens\n")
      msg.append("=================================\n")
      service.clean
        .map { deleted ⇒
          val seconds = (clock.now.getMillis - start) / 1000
          msg
            .append("Total of %s auth tokens(s) were deleted in %s seconds".format(deleted.length, seconds))
            .append("\n")
          msg.append("=================================\n")

          msg.append("=================================\n")
          log.info(msg.toString)
        }
        .recover {
          case e ⇒
            msg.append("Couldn't cleanup auth tokens because of unexpected error\n")
            msg.append("=================================\n")
            log.error(msg.toString, e)
        }
  }
}

/**
 * The companion object.
 */
object AuthTokenCleaner {
  case object Clean
}
