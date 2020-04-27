package de.athalis.sbt.testcoreasm

import org.scalatest.Matchers._

import scala.language.implicitConversions

object Util {
  class AssertionHolder(f: => Any) {
    def withMessage(s: String): Any = {
      withClue(s) { f }
    }
  }

  implicit def convertAssertion(f: => Any): AssertionHolder = new AssertionHolder(f)
}
