name := "yJunction"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.2",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.2" % Test
)

libraryDependencies += "com.typesafe" % "config" % "1.3.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.28"
