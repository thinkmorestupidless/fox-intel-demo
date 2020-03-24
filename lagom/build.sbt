import Dependencies._

organization in ThisBuild := "com.example"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

lazy val `mailbox` = (project in file("."))
  .aggregate(
    `newsletter-topic-api`,
    `transaction-topic-api`,
    `mailbox-api`, `mailbox-impl`,
    `user-api`, `user-service`
  )

lazy val `newsletter-topic-api` = (project in file("newsletter-topic-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `transaction-topic-api` = (project in file("transaction-topic-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `mailbox-api` = (project in file("mailbox-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `mailbox-impl` = (project in file("mailbox-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceJdbc,
      postgresDriver,
      lagomScaladslKafkaBroker,
      lagomScaladslAkkaDiscovery,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`mailbox-api`, `newsletter-topic-api`, `transaction-topic-api`)

lazy val `mailbox-stream-api` = (project in file("mailbox-stream-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `mailbox-stream-impl` = (project in file("mailbox-stream-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`mailbox-stream-api`, `mailbox-api`)

lazy val `user-api` = (project in file("user-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playSilhouette
    )
  )
  .settings(commonSettings)

lazy val `user-service` = (project in file("user-impl"))
  .enablePlugins(LagomScala, DockerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceJdbc,
      postgresDriver,
      lagomScaladslKafkaBroker,
      lagomScaladslAkkaDiscovery,
      akkaDiscoveryKubernetesApi,
      lagomScaladslTestKit,
      macwire,
      scalaTest
    )
  )
  .settings(
    lagomServiceHttpPort := 11002
  )
  .settings(lagomForkedTestSettings)
  .settings(commonSettings)
  .settings(dockerSettings)
  .dependsOn(`user-api`)

//lazy val `service-gateway` = (project in file("service-gateway"))
//  .enablePlugins(PlayScala, LagomScala)
//  .settings(
//    libraryDependencies ++= Seq(
//      lagomScaladslClient,
//      lagomScaladslAkkaDiscovery,
//      akkaDiscoveryKubernetesApi,
//      lagomScaladslDevMode,
//      guice,
//      ehcache,
//      filters,
//      playMailer,
//      playMailerGuice,
//      playSlick,
//      postgresDriver,
//      playSilhouette,
//      playSilhouetteBcrypt,
//      playSilhouetteCrypto,
//      playSilhouettePersistence,
//      scalaGuice,
//      akkaQuartzScheduler,
//      ficus,
//      scalaTest
//    )
//  )
//  .settings(
//    lagomServiceHttpPort := 9000
//  )
//  .settings(commonSettings)
//  .settings(dockerSettings)
//  .dependsOn(`user-api`)

def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := getDockerBaseImage(),
  dockerUsername := sys.props.get("docker.username"),
  dockerRepository := sys.props.get("docker.repository"),
  dockerExposedPorts := Seq(8080, 8558, 2550, 9000, 9001)
)

def getDockerBaseImage(): String = sys.props.get("java.version") match {
  case Some(v) if v.startsWith("11") => "adoptopenjdk/openjdk11"
  case _ => "adoptopenjdk/openjdk8"
}

lazy val commonSettings = Seq(
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls",
    "-Xlint",
    "-Ywarn-unused",
    "-Ywarn-unused-import",
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked"
  ),

  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value
)

version in ThisBuild ~= (_.replace('+', '-'))
