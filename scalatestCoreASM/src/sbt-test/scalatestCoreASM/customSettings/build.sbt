lazy val root = (project in file("."))
  .aggregate(scalatestCoreASMExample)
  .settings(Commons.settings)

lazy val scalatestCoreASMExample = (project in file("scalatestCoreASMExample"))
  .settings(Commons.settings)
  .enablePlugins(ScalatestCoreASMPlugin)
  .settings(
    name := "sbt-scalatest-coreasm-example",
    scalaVersion := Commons.appScalaVersion_2_12,

    testCoreASMPackageName in TestCoreASM := "com.example",

    sourceDirectory in TestCoreASM := file("./customSource"),
    includeFilter in TestCoreASM := "*.customCoreasm",
    sourceManaged in TestCoreASM := file("./customSourceManaged")
  )
