package jobs

import akka.actor.{ ActorRef, ActorSystem }
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import javax.inject.{ Inject, Named }

/**
 * Schedules the jobs.
 */
class Scheduler @Inject() (
    system: ActorSystem,
    @Named("auth-token-cleaner") authTokenCleaner: ActorRef
) {

  QuartzSchedulerExtension(system).schedule("AuthTokenCleaner", authTokenCleaner, AuthTokenCleaner.Clean)

  authTokenCleaner ! AuthTokenCleaner.Clean
}
