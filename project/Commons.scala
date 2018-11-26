import sbt._
import Keys._

object Commons {
  val appVersion = "0.1.0-SNAPSHOT"

  val appScalaVersion_2_10 = "2.10.7"
  val appScalaVersion_2_11 = "2.11.12"
  val appScalaVersion_2_12 = "2.12.7"

  val settings = Def.settings(
    version := appVersion,
    scalaVersion := appScalaVersion_2_10,
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding", "utf8",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Xfuture",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Xfatal-warnings")
  )
}
