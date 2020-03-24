package com.example.mailbox.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.example.transaction.{NewsletterEventService, NewsletterReceived}

import scala.concurrent.duration._

class NewsletterEventSubscriber(clusterSharding: ClusterSharding, newsletters: NewsletterEventService) {

  implicit val timeout = Timeout(5.seconds)

  newsletters.newslettersReceived().subscribe.atLeastOnce(
    Flow[NewsletterReceived].mapAsync(1) { newsletter =>
      val ref = clusterSharding.entityRefFor(MailboxState.typeKey, newsletter.header.id)

      ref.ask[Done](replyTo => AddNewsletterToMailbox(newsletter, replyTo))
    }
  )

}
