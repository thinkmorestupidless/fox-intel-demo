package com.example.mailboxstream.impl

import com.example.mailbox.api.MailboxService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.example.mailboxstream.api.MailboxStreamService

import scala.concurrent.Future

/**
  * Implementation of the MailboxStreamService.
  */
class MailboxStreamServiceImpl(mailboxService: MailboxService) extends MailboxStreamService {
  def stream = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(mailboxService.hello(_).invoke()))
  }
}
