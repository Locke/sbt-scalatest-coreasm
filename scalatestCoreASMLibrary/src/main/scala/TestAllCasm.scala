package de.athalis.sbt.testcoreasm

/*

This is based on: https://github.com/CoreASM/coreasm.core/blob/master/org.coreasm.engine/test/org/coreasm/engine/test/TestAllCasm.java @ 3ad4954

*/

import java.io.{FileSystem => _, _}
import java.nio.file._
import java.util.regex.Pattern

import scala.collection.JavaConversions._
import org.scalatest._

import org.coreasm.util.Tools

object TestAllCasm {
  def getFilteredOutput(file: Path, filter: String): Seq[String] = {
    var filteredOutputList = Seq.empty[String]
    val pattern = Pattern.compile(filter + ".*")

    var input: BufferedReader = null
    try {
      input = new BufferedReader(new InputStreamReader(Files.newInputStream(file)))
      var line: String = null

      while ({line = input.readLine(); line != null}) {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
          val first = line.indexOf("\"", matcher.start) + 1
          val last = line.indexOf("\"", first)
          if (last > first) {
            filteredOutputList +:= Tools.convertFromEscapeSequence(line.substring(first, last))
          }
        }
      }
    }
    catch {
      case e: FileNotFoundException => e.printStackTrace()
      case e: IOException => e.printStackTrace()
    }
    finally {
      if (input != null)
        input.close()
    }

    filteredOutputList
  }

  def getParameter(file: Path, name: String): Option[Int] = {
    var value: Option[Int] = None
    val pattern = Pattern.compile("@" + name + "\\s*(\\d+)")

    var input: BufferedReader = null
    try {
      input = new BufferedReader(new InputStreamReader(Files.newInputStream(file)))
      var line: String = null
      var found = false
      while (!found && {line = input.readLine(); line != null}) {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
          value = Some(Integer.parseInt(matcher.group(1)))
          found = true
        }
      }
    }
    catch {
      case e: FileNotFoundException => e.printStackTrace()
      case e: IOException => e.printStackTrace()
    }
    finally {
      if (input != null)
        input.close()
    }

    value
  }
}

//class TestAllCasm extends FunSuite with Matchers {
abstract class TestAllCasm extends FunSuite with Matchers with Checkpoints {
  import TestAllCasm._
  import Util._

  def outFilter(in: String): Boolean = false
  def logFilter(in: String): Boolean = false
  def errFilter(in: String): Boolean = false
  def failOnWarning: Boolean = true

  def testFiles: Seq[Path]

  test ("initialize") {
    ( testFiles should not be empty ) withMessage ("testFiles: ")
  }

  for (testFile <- testFiles) {
    test(testFile.toString) {
      val origOutput: java.io.PrintStream = System.out
      val origError: java.io.PrintStream = System.err

      val logStream = new ByteArrayOutputStream()
      val outStream = new ByteArrayOutputStream()
      val errStream = new ByteArrayOutputStream()

      try {
        System.setOut(new PrintStream(logStream))
        System.setErr(new PrintStream(errStream))

        outStream.reset()
        logStream.reset()
        errStream.reset()

        runSpecification(testFile, outStream, logStream, errStream, origOutput)

        outStream.reset()
        logStream.reset()
        errStream.reset()
      }
      finally {
        System.setOut(origOutput)
        System.setErr(origError)
      }
    }
  }

  private def runSpecification(testFile: Path, outStream: ByteArrayOutputStream, logStream: ByteArrayOutputStream, errStream: ByteArrayOutputStream, origOutput: PrintStream): Unit = {
    var requiredOutputList = getFilteredOutput(testFile, "@require")
    val refusedOutputList = getFilteredOutput(testFile, "@refuse")
    val minSteps = getParameter(testFile, "minsteps").getOrElse(1)
    val maxSteps = getParameter(testFile, "maxsteps").getOrElse(minSteps)

    val td = TestEngineDriver.newLaunch(testFile, null)

    td.setOutputStream(new PrintStream(outStream))

    for (step <- 0 to maxSteps if (step < minSteps || !requiredOutputList.isEmpty)) {

      if (td.getStatus == TestEngineDriver.TestEngineDriverStatus.stopped) {
        ( requiredOutputList shouldBe empty ) withMessage ("output:\n" + outStream.toString + "\n\nerrors:\n" + errStream.toString + "\n\nEngine terminated after " + step + " steps, but is missing required output: ")
        ( step should be >= minSteps ) withMessage ("output:\n" + outStream.toString + "\n\nerrors:\n" + errStream.toString + "\n\nEngine terminated after " + step + " steps: ")
      }

      outStream.reset()
      logStream.reset()
      errStream.reset()

      td.executeSteps(1)

      val outputOut = outStream.toString
      val outputLog = logStream.toString
      val outputErr = errStream.toString

      for (line <- outputOut.lines.filter(outFilter)) {
        origOutput.println("out: " + line)
      }

      for (line <- outputLog.lines.filter(logFilter)) {
        origOutput.println("log" + line)
      }

      for (line <- outputErr.lines.filter(errFilter)) {
        origOutput.println("err: " + line)
      }


      //check for refused output / errors. report all errors
      val cp = new Checkpoint

      cp {
        //test if no error has occurred and maybe output error message
        (outputErr shouldBe empty) withMessage ("output:\n" + outputOut + "\n\nEngine had an error after " + step + " steps: ")
      }

      if (failOnWarning) cp {
        //test if no error has occurred and maybe output error message
        (outputLog should not include "WARN") withMessage ("output:\n" + outputOut + "\n\nEngine had an warning after " + step + " steps: ")
      }

      for (refusedOutput <- refusedOutputList) {
        cp {
          outputOut.lines.filter(_.contains(refusedOutput)).toSeq shouldBe empty withMessage ("output: \n" + outputOut)
        }
      }
      cp.reportAll()


      requiredOutputList = requiredOutputList.filterNot { x => outputOut.contains(x) }
    }

    //check if no required output is missing
    ( requiredOutputList shouldBe empty ) withMessage (outStream.toString + "\n\nremaining required output: ")

    td.stop()
  }
}

