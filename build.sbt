lazy val root = project
  .in(file("."))
  .settings(
    name := "polly",
    version := "0.1.0",

    scalaVersion := "3.0.0-RC1",

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",

    useScala3doc := true,
  )