package com.example.cloudflow

import java.util.UUID

import com.example.data.MailHeader
import com.github.javafaker.Faker

import scala.concurrent.Future
import scala.util.Random

object FakeStuff {

  val TOTAL = 1000
  val faker = new Faker()
  val random = new Random()

  val users = (0 to TOTAL - 3).map { _ =>
    User(UUID.randomUUID().toString, faker.internet().emailAddress())
  }.toList :+ User(UUID.randomUUID().toString, "user@newsletter.com") :+ User(UUID.randomUUID().toString, "user@transaction.com")

  def randomUser =
    users(random.nextInt(TOTAL))

  def randomEmail =
    randomUser.email

  def randomHeaders(id: String, email: String) =
    (0 to random.nextInt(100)).map { _ =>
      MailHeader(id, randomEmail, email, System.currentTimeMillis())
    }

  def asyncHeaders(id: String, email: String) =
    Future.successful(randomHeaders(id, email))
}

case class User(uuid: String, email: String)
