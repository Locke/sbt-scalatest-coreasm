package de.athalis.sbt.testcoreasm

import sbt._
import sbt.Keys._

import java.nio.file.Path

object ScalatestCoreASMPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  object autoImport {
    lazy val TestCoreASM = config("test-coreasm") extend (Test)

    lazy val generateCoreASMTests = taskKey[Seq[File]]("generate scalatest classes for CoreASM specifications")
  }

  import autoImport._

  lazy val baseTestCoreASMSettings = Def.settings(
    sourceDirectory := (sourceDirectory in Test) { _ / "coreasm" }.value,
    sourceManaged := (sourceManaged in Test) { _ / "coreasm" }.value,

    includeFilter := ("*.casm" || "*.coreasm"),
    excludeFilter := HiddenFileFilter,

    sources := (sourceDirectory.value ** (includeFilter.value -- excludeFilter.value)).get,

    generateCoreASMTests := {
      val cachedFun = FileFunction.cached(streams.value.cacheDirectory, FilesInfo.lastModified, FilesInfo.lastModified) {
        (in: Set[File]) =>
          Generator(sourceDirectory.value, in, sourceManaged.value) : Set[File]
      }
      cachedFun(sources.value.toSet).toSeq
    }
  )

  override lazy val projectSettings =
    inConfig(TestCoreASM)(baseTestCoreASMSettings) ++
    Def.settings(
      libraryDependencies ++= Seq("de.athalis" %% "sbt-scalatest-coreasm-lib" % BuildInfo.version % "test"),

      sourceGenerators in Test += (generateCoreASMTests in TestCoreASM),
      (managedSourceDirectories in Test) += (sourceManaged in TestCoreASM).value,
      (unmanagedResourceDirectories in Test) += (sourceDirectory in TestCoreASM).value,
      cleanFiles += (sourceManaged in TestCoreASM).value
    )
}

object Generator {
  private val info = "%s, version: %s, scalaVersion: %s, sbtVersion: %s" format (BuildInfo.name, BuildInfo.version, BuildInfo.scalaVersion, BuildInfo.sbtVersion)

  private val template = 
"""package de.athalis.sbt.testcoreasm

/*
This file is automatically generated by %s.
Do not modify this file -- YOUR CHANGES WILL BE ERASED!
*/

import java.io.{ File => JFile }
import java.nio.file.{ Path => JPath }

class %s extends TestAllCasm {
  def testFiles: Seq[JPath] = %s
}
"""

  def apply(srcDir: File, files: Set[File], outDir: File): Set[File] = {
    val m: Map[String, Set[File]] = toMap(srcDir, files)

    m.map(x => {
        val parentFolder = x._1
        val files = x._2
        generate(files, outDir, parentFolder)
      }).toSet.flatten
  }

  private def toMap(srcDir: File, files: Set[File]): Map[String, Set[File]] = {
    val srcDirPath = srcDir.toPath

    val x: Set[(String, File)] = files.map(f => {
        val relativized: Path = srcDirPath.relativize(f.toPath)
        val parent: Path = relativized.getParent
        val x: String = if (parent == null) "WithoutTestClasses" else parent.getName(0).toString
        (x, f)
      })

    val keys: Set[String] = x.map(_._1)
    val m: Map[String, Set[File]] = keys.map(key => {
        val files: Set[File] = x.filter(_._1 == key).map(_._2)
        (key, files)
      }).toMap

    m
  }

  private def generate(files: Set[File], outDir: File, parentFolder: String): Set[File] = {
    val filesAbsolute = files.map(f => "new JFile(\"\"\""+f.getCanonicalPath+"\"\"\")")

    val filesSeq = filesAbsolute.mkString("Seq(", ", ", ").map(_.toPath)")

    val source = template format (info, "Test" + parentFolder, filesSeq)

    val outFile = outDir / ("Test" + parentFolder + ".scala")
    IO.write(outFile, source)

    Set(outFile)
  }
}

