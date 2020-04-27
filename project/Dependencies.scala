import sbt._

object Dependencies {
  val coreASMEngineVersion = "1.7.3-locke-4"
  val coreASMEngine = "de.athalis.coreasm" % "coreasm-engine" % coreASMEngineVersion

  val scalaTestVersion = "3.0.5"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion

  val scalatestCoreASMLibraryDependencies: Seq[ModuleID] = Seq(
    coreASMEngine,
    scalaTest
  )

  val scalatestCoreASMDependencies: Seq[ModuleID] = Seq()
}
