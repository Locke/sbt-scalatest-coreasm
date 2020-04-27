package de.athalis.sbt.testcoreasm

import java.io.StringReader

import org.scalatest.{FunSuite, Matchers}

class TestTestSettings  extends FunSuite with Matchers {

  private val spec =
    """
      |/*
      |@require "A"
      | @require "B"
      |*/
      |// @require "C"
      |// @require "D" // comment
      |@refuse "X"
      |@refuse "Y"
      |@minsteps 1
      |@maxsteps 7
      |@minsteps 3
      |
      |init main
    """.stripMargin

  test("StringReader") {
    val reader = new StringReader(spec)

    val testSettings = TestSettings.readTestSettings(reader)

    testSettings.require shouldBe Set("A", "B", "C", "D")
    testSettings.refuse shouldBe Set("X", "Y")
    testSettings.minSteps shouldBe 3
    testSettings.maxSteps shouldBe 7
  }
}
