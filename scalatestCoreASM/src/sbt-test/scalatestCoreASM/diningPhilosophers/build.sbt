lazy val root = (project in file("."))
  .aggregate(scalatestCoreASMExample)
  .settings(Commons.settings)

lazy val scalatestCoreASMExample = (project in file("scalatestCoreASMExample"))
  .settings(Commons.settings)
  .enablePlugins(ScalatestCoreASMPlugin)
  .settings(
    name := "sbt-scalatest-coreasm-example",
    scalaVersion := Commons.appScalaVersion_2_12,
    crossScalaVersions := Seq(Commons.appScalaVersion_2_10, Commons.appScalaVersion_2_11, Commons.appScalaVersion_2_12)
  )
