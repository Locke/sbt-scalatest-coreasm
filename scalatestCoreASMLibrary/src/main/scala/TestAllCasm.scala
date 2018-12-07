package de.athalis.sbt.testcoreasm

/*

This is based on: https://github.com/CoreASM/coreasm.core/blob/master/org.coreasm.engine/test/org/coreasm/engine/test/TestAllCasm.java @ 3ad4954

*/

import java.io.{FileSystem => _, _}

import org.scalatest._

//class TestAllCasm extends FunSuite with Matchers {
abstract class TestAllCasm extends FunSuite with Matchers with Checkpoints {
  import Util._

  def outFilter(in: String): Boolean = false
  def logFilter(in: String): Boolean = false
  def errFilter(in: String): Boolean = false
  def failOnWarning: Boolean = true

  def testFileNames: Seq[String]
  def getTestFileReader(testFileName: String): Reader

  test ("initialize") {
    ( testFileNames should not be empty ) withMessage ("testFiles: ")
  }

  for (testFileName <- testFileNames) {
    test(testFileName) {
      runSpecification(testFileName)
    }
  }

  private def runSpecification(testFileName: String): Unit = synchronized {
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

      runSpecification(testFileName, outStream, logStream, errStream, origOutput)

      outStream.reset()
      logStream.reset()
      errStream.reset()
    }
    finally {
      System.setOut(origOutput)
      System.setErr(origError)
    }
  }

  private def runSpecification(testFileName: String, outStream: ByteArrayOutputStream, logStream: ByteArrayOutputStream, errStream: ByteArrayOutputStream, origOutput: PrintStream): Unit = {

    val srcReader1: Reader = getTestFileReader(testFileName) // NOTE: currently closed in readTestSettings
    val testSettings: TestSettings = TestSettings.readTestSettings(srcReader1)
    var requiredOutputList = testSettings.require

    val srcReader2: Reader = getTestFileReader(testFileName) // TODO: close?
    val td = TestEngineDriver.newLaunch(testFileName, srcReader2, null)

    try {
      td.setOutputStream(new PrintStream(outStream))

      for (step <- 0 to testSettings.maxSteps if (step < testSettings.minSteps || !requiredOutputList.isEmpty)) {

        if (td.getStatus == TestEngineDriver.TestEngineDriverStatus.stopped) {
          (requiredOutputList shouldBe empty) withMessage ("output:\n" + outStream.toString + "\n\nerrors:\n" + errStream.toString + "\n\nEngine terminated after " + step + " steps, but is missing required output: ")
          (step should be >= testSettings.minSteps) withMessage ("output:\n" + outStream.toString + "\n\nerrors:\n" + errStream.toString + "\n\nEngine terminated after " + step + " steps: ")
        }

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
          //test if no unexpected error has occurred
          var errors: Iterator[String] = outputErr.lines
          errors = errors.filterNot(msg => msg.contains("SLF4J") && msg.contains("binding"))
          (errors.toSeq shouldBe empty) withMessage ("log:\n" + outputLog + "\n\noutput:\n" + outputOut + "\n\nEngine had an error after " + step + " steps: ")
        }

        if (failOnWarning) cp {
          //test if no unexpected warning has occurred
          var warnings = outputLog.lines.filter(_.contains("WARN"))
          warnings = warnings.filterNot(msg => msg.contains("The update was not successful so it might not be added to the universe."))
          warnings = warnings.filterNot(msg => msg.contains("org.coreasm.util.Tools") && msg.toLowerCase.contains("root folder"))
          (warnings.toSeq shouldBe empty) withMessage ("output:\n" + outputOut + "\n\nEngine had an warning after " + step + " steps: ")
        }

        for (refusedOutput <- testSettings.refuse) {
          cp {
            outputOut.lines.filter(_.contains(refusedOutput)).toSeq shouldBe empty withMessage ("output: \n" + outputOut)
          }
        }
        cp.reportAll()


        requiredOutputList = requiredOutputList.filterNot { x => outputOut.contains(x) }

        outStream.reset()
        logStream.reset()
        errStream.reset()
      }

      //check if no required output is missing
      (requiredOutputList shouldBe empty) withMessage (outStream.toString + "\n\nremaining required output after " + testSettings.maxSteps + " maxSteps: ")
    }
    finally {
      td.stop()
    }
  }
}
