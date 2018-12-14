package org.piggottfamily.cbb_explorer.utils

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._

object TestUtils {

  /** Like a match statement but throws an error if no cases apply */
  def inside[T](t: => T)(pf: PartialFunction[T, Unit]): Unit = {
    val fallthrough: PartialFunction[T, Unit] = {
      case non_matching =>
        throw new Exception(s"Failed to match inside [$non_matching]")
    }
    try {
      (pf orElse fallthrough).apply(t)
    } catch {
      case assert: java.lang.AssertionError =>
        val new_message = s"ASSERT inside [$t]:\n${assert.getMessage}"
        throw new java.lang.AssertionError(new_message, assert.getCause)
    }
  }

  /** Converts HTML string into a doc */
  def get_doc(html: String): Document = {
    val browser = JsoupBrowser()
    browser.parseString(html)
  }
  /** Converts HTML string into a doc (fixture format) */
  def with_doc(html: String)(test: Document => Unit): Unit = {
    val doc = get_doc(html)
    test(doc)
  }
}
