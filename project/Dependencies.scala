import sbt._
import Keys._

object Dependencies {
  val coreASMEngineVersion = "1.7.3-SNAPSHOT"
  val coreASMEngine = "org.coreasm" % "org.coreasm.engine" % coreASMEngineVersion

  val scalaTestVersion = "3.0.0-RC4"
  //val scalaTestVersion = "2.2.6"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion

  val scalatestCoreASMLibraryDependencies: Seq[ModuleID] = Seq(
    coreASMEngine,
    scalaTest
  )

  val scalatestCoreASMDependencies: Seq[ModuleID] = Seq()
}
