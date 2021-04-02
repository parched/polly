val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "polly",
    version := "0.1.0",
    scalaVersion := "3.0.0-RC1",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
    ).map(_.cross(CrossVersion.for3Use2_13)),
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",

    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.5",
    libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.5" % "test",
  )
