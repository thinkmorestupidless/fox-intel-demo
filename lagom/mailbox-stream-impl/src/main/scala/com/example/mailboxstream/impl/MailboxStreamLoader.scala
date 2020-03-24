package com.example.mailboxstream.impl

import com.example.mailbox.api.MailboxService
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.example.mailboxstream.api.MailboxStreamService
import com.softwaremill.macwire._

class MailboxStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new MailboxStreamApplication(context) {
      override def serviceLocator: NoServiceLocator.type = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new MailboxStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[MailboxStreamService])
}

abstract class MailboxStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[MailboxStreamService](wire[MailboxStreamServiceImpl])

  // Bind the MailboxService client
  lazy val mailboxService: MailboxService = serviceClient.implement[MailboxService]
}
