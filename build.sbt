name := "maze-backend-challenge"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",

  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
)