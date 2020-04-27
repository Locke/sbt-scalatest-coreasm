lazy val root = (project in file("."))
  .aggregate(scalatestCoreASMLibrary, scalatestCoreASM)
  .settings(Commons.settings)
  .settings(
    organization := "de.athalis",
    name := "sbt-scalatest-coreasm-root",
  )

lazy val scalatestCoreASMLibrary = (project in file("scalatestCoreASMLibrary"))
  .settings(Commons.settings)
  .settings(
    organization := "de.athalis",
    name := "sbt-scalatest-coreasm-lib",
    scalaVersion := Commons.appScalaVersion_2_12,
    crossScalaVersions := Seq(Commons.appScalaVersion_2_10, Commons.appScalaVersion_2_11, Commons.appScalaVersion_2_12),
    publishArtifact in (Compile, packageDoc) := false,
    libraryDependencies ++= Dependencies.scalatestCoreASMLibraryDependencies,
  )

lazy val scalatestCoreASM = (project in file("scalatestCoreASM"))
  .enablePlugins(BuildInfoPlugin, SbtPlugin)
  .dependsOn(scalatestCoreASMLibrary)
  .settings(Commons.settings)
  .settings(
    organization := "de.athalis",
    name := "sbt-scalatest-coreasm",
    // sbt 1 is compiled against 2.12
    scalaVersion := Commons.appScalaVersion_2_12,
    crossScalaVersions := Seq(Commons.appScalaVersion_2_12),
    libraryDependencies ++= Dependencies.scalatestCoreASMDependencies,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.athalis.sbt.testcoreasm",
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
  )
