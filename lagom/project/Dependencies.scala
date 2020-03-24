import sbt._

object Versions {

  val slf4jApiVersion             = "1.7.28"
  val janinoVersion               = "3.0.6"
  val playJsonVersion             = "2.7.4"
  val playSlickVersion            = "3.0.0"
  val playMailerVersion           = "6.0.1"
  val akkaQuartzSchedulerVersion  = "1.8.2-akka-2.6.x"
  val akkaManagementVersion       = "1.0.0"
  val akkaDiscoveryEC2Version     = "1.0.5"
  val silhouetteVersion           = "6.1.1"
  val ficusVersion                = "1.4.7"
  val macwireVersion              = "2.3.3"
  val scalaGuiceVersion           = "4.2.6"
  val twilioVersion               = "7.42.0"
  val postgresDriverVersion       = "42.1.4"
  val scalatestVersion            = "3.0.8"
  val jfakerVersion               = "1.0.1"
}


object Dependencies {
  import Versions._

  val slf4jApi                    = "org.slf4j"                     % "slf4j-api"                                       % slf4jApiVersion
  val janino                      = "org.codehaus.janino"           % "janino"                                          % janinoVersion
  val playJson                    = "com.typesafe.play"            %% "play-json"                                       % playJsonVersion
  val playSlick                   = "com.typesafe.play"            %% "play-slick"                                      % playSlickVersion
  val playMailer                  = "com.typesafe.play"            %% "play-mailer"                                     % playMailerVersion
  val playMailerGuice             = "com.typesafe.play"            %% "play-mailer-guice"                               % playMailerVersion
  val akkaQuartzScheduler         = "com.enragedginger"            %% "akka-quartz-scheduler"                           % akkaQuartzSchedulerVersion
  val akkaDiscoveryKubernetesApi  = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api"                   % akkaManagementVersion
  val akkaDiscoveryEC2            = "com.lightbend.akka.discovery" %% "akka-discovery-aws-api"                          % akkaDiscoveryEC2Version
  val playSilhouette              = "com.mohiva"                   %% "play-silhouette"                                 % silhouetteVersion
  val playSilhouetteBcrypt        = "com.mohiva"                   %% "play-silhouette-password-bcrypt"                 % silhouetteVersion
  val playSilhouettePersistence   = "com.mohiva"                   %% "play-silhouette-persistence"                     % silhouetteVersion
  val playSilhouetteCrypto        = "com.mohiva"                   %% "play-silhouette-crypto-jca"                      % silhouetteVersion
  val playSilhouetteTestkit       = "com.mohiva"                   %% "play-silhouette-testkit"                         % silhouetteVersion
  val ficus                       = "com.iheart"                   %% "ficus"                                           % ficusVersion
  val macwire                     = "com.softwaremill.macwire"     %% "macros"                                          % macwireVersion          % Provided
  val scalaGuice                  = "net.codingwell"               %% "scala-guice"                                     % scalaGuiceVersion
  val twilio                      = "com.twilio.sdk"                % "twilio"                                          % twilioVersion
  val postgresDriver              = "org.postgresql"                % "postgresql"                                      % postgresDriverVersion
  val scalaTest                   = "org.scalatest"                %% "scalatest"                                       % scalatestVersion        % Test
  val jfaker                      = "com.github.javafaker"          % "javafaker"                                       % jfakerVersion           % Test
}
