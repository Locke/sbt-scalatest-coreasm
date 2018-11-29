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
    sourceDirectory := (sourceDirectory in Test).value / "coreasm",
    sourceManaged := (sourceManaged in Test).value / "coreasm",

    includeFilter := ("*.casm" || "*.coreasm"),
    excludeFilter := HiddenFileFilter,

    sources := (sourceDirectory.value ** (includeFilter.value -- excludeFilter.value)).get,

    generateCoreASMTests := {
      val log = streams.value.log
      val cachedFun = FileFunction.cached(streams.value.cacheDirectory, FilesInfo.lastModified, FilesInfo.lastModified) {
        (in: Set[File]) =>
          Generator(sourceDirectory.value, in, sourceManaged.value, log) : Set[File]
      }
      cachedFun(sources.value.toSet).toSeq
    }
  )

  override lazy val projectSettings =
    inConfig(TestCoreASM)(baseTestCoreASMSettings) ++
    Def.settings(
      libraryDependencies ++= Seq("de.athalis" %% "sbt-scalatest-coreasm-lib" % BuildInfo.version % "test"),

      sourceGenerators in Test += (generateCoreASMTests in TestCoreASM),
      managedSourceDirectories in Test += (sourceManaged in TestCoreASM).value,
      unmanagedResourceDirectories in Test += (sourceDirectory in TestCoreASM).value,
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

  def apply(srcDirIn: File, filesIn: Set[File], outDirIn: File, log: Logger): Set[File] = {
    val srcDir: File = srcDirIn.getCanonicalFile
    val files: Set[File] = filesIn.map(_.getCanonicalFile)
    val outDir: File = outDirIn.getCanonicalFile

    log.info("ScalatestCoreASMPlugin: generating test files..")
    log.debug("srcDir: " + srcDir)
    log.debug("files: " + files)
    log.debug("outDir: " + outDir)
	
    val testNamesWithFiles: Map[String, Set[File]] = toMap(srcDir, files)

    log.debug("testNamesWithFiles: " + testNamesWithFiles)

    val generatedFiles: Set[File] = testNamesWithFiles.map(x => {
        val testName = x._1
        val files = x._2
        generate(files, outDir, testName)
      }).toSet.flatten

    log.debug("generatedFiles: " + generatedFiles)

    generatedFiles
  }

  private def toMap(srcDir: File, files: Set[File]): Map[String, Set[File]] = {
    val filesWithTestName: Set[(File, String)] = files.map(f => {
        val testName: String = if (f.getParentFile.equals(srcDir)) {
          "WithoutTestClasses"
        }
        else {
          f.getParentFile.getName
        }

        (f, testName)
      })

    val testNames: Set[String] = filesWithTestName.map(_._2)

    val testNamesWithFiles: Map[String, Set[File]] = testNames.map(key => {
        val files: Set[File] = filesWithTestName.filter(_._2 == key).map(_._1)
        (key, files)
      }).toMap

    testNamesWithFiles
  }

  private def generate(files: Set[File], outDir: File, testName: String): Set[File] = {
    val filesAbsolute = files.map(f => "new JFile(\"\"\""+f.getCanonicalPath+"\"\"\")")

    val filesSeq = filesAbsolute.mkString("Seq(", ", ", ").map(_.toPath)")

    val source = template format (info, "Test" + testName, filesSeq)

    val outFile = outDir / ("Test" + testName + ".scala")
    IO.write(outFile, source)

    Set(outFile)
  }
}

