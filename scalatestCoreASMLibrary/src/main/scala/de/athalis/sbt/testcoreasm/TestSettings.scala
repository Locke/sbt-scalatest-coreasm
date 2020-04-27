package de.athalis.sbt.testcoreasm

import java.io.{BufferedReader, Reader}
import java.util.regex.Pattern

import org.coreasm.util.Tools

import scala.collection.JavaConverters._

object TestSettings {

  // foo\s*"((\\"|[^"])+)" -> foo followed by whitespace, then any text, including \", between two "

  private val requirePattern  = Pattern.compile("@require\\s*\"((\\\\\"|[^\"])+)\"")
  private val refusePattern   = Pattern.compile("@refuse.\\s*\"([^\"]+)\"")
  private val minStepsPattern = Pattern.compile("@minsteps\\s*(\\d+)")
  private val maxStepsPattern = Pattern.compile("@maxsteps\\s*(\\d+)")

  private def readRequire(target: scala.collection.mutable.ListBuffer[String], line: String): Boolean = {
    readEscapedString(target, line, requirePattern)
  }

  private def readRefuse(target: scala.collection.mutable.ListBuffer[String], line: String): Boolean = {
    readEscapedString(target, line, refusePattern)
  }

  private def readMinSteps(target: scala.collection.mutable.ListBuffer[Int], line: String): Boolean = {
    readDecimal(target, line, minStepsPattern)
  }

  private def readMaxSteps(target: scala.collection.mutable.ListBuffer[Int], line: String): Boolean = {
    readDecimal(target, line, maxStepsPattern)
  }

  private def readEscapedString(target: scala.collection.mutable.ListBuffer[String], line: String, pattern: Pattern): Boolean = {
    val matcher = pattern.matcher(line)

    if (matcher.find()) {
      val plain: String = matcher.group(1)

      val converted = Tools.convertFromEscapeSequence(plain)

      target.prepend(converted)

      true
    }
    else {
      false
    }
  }

  private def readDecimal(target: scala.collection.mutable.ListBuffer[Int], line: String, pattern: Pattern): Boolean = {
    val matcher = pattern.matcher(line)

    if (matcher.find()) {
      val plain: String = matcher.group(1)

      val parsed = Integer.parseInt(plain)

      target.prepend(parsed)

      true
    }
    else {
      false
    }
  }

  def readTestSettings(in: Reader): TestSettings = {
    val inReader = new BufferedReader(in)

    try {
      val lines: Iterator[String] = inReader.lines().iterator().asScala.filter(_.contains("@"))

      readTestSettings(lines)
    }
    finally {
      inReader.close()
      in.close()
    }
  }

  def readTestSettings(lines: Iterator[String]): TestSettings = {
    val requires    = new scala.collection.mutable.ListBuffer[String]()
    val refuse      = new scala.collection.mutable.ListBuffer[String]()
    val allMinSteps = new scala.collection.mutable.ListBuffer[Int]()
    val allMaxSteps = new scala.collection.mutable.ListBuffer[Int]()

    lines.foreach(line => {
      if (readRequire(requires, line)) {
        // done
      }
      else if (readRefuse(refuse, line)) {
        // done
      }
      else if (readMinSteps(allMinSteps, line)) {
        // done
      }
      else if (readMaxSteps(allMaxSteps, line)) {
        // done
      }
      else {
        // unknown setting
      }
    })

    val minSteps: Int = allMinSteps.headOption.getOrElse(1)
    val maxSteps: Int = allMaxSteps.headOption.getOrElse(minSteps)

    TestSettings(requires.toSet, refuse.toSet, minSteps, maxSteps)
  }
}

case class TestSettings(require: Set[String], refuse: Set[String], minSteps: Int, maxSteps: Int)
