import sbt._
import Keys._

object Dependencies {
  val coreASMEngineVersion = "1.7.3"
  val coreASMEngine = "org.coreasm" % "org.coreasm.engine" % coreASMEngineVersion

  val scalaTestVersion = "3.0.5"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion

  val scalatestCoreASMLibraryDependencies: Seq[ModuleID] = Seq(
    coreASMEngine,
    scalaTest
  )

  val scalatestCoreASMDependencies: Seq[ModuleID] = Seq()
}
