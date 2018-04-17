package org.piggottfamily.cbb_explorer.utils.parsers

import utest._
import org.piggottfamily.cbb_explorer.utils.TestUtils

object ParseUtilsTests extends TestSuite {
  val tests = Tests {
    "ParserUtils" - {
      "parse_score" - {
        TestUtils.inside(ParseUtils.parse_score(Some("1.1"))) {
          case Right(1.1) =>
        }
        // (Also check can handle symbols)
        TestUtils.inside(ParseUtils.parse_score(Some(" +1.1%"))) {
          case Right(1.1) =>
        }
        TestUtils.inside(ParseUtils.parse_score(Some("-1yrs"))) {
          case Right(-1.0) =>
        }
        // Errors
        TestUtils.inside(ParseUtils.parse_score(None)) {
          case Left(ParseError("", "[value]", _)) =>
        }
        TestUtils.inside(ParseUtils.parse_score(Some("rabbit"))) {
          case Left(ParseError("", "[value]", _)) =>
        }
      }
      "parse_rank" - {
        TestUtils.inside(ParseUtils.parse_rank(Some("2"))) {
          case Right(2) =>
        }
        TestUtils.inside(ParseUtils.parse_rank(Some("2.2"))) {
          case Left(ParseError("", "[rank]", _)) =>
        }
        TestUtils.inside(ParseUtils.parse_rank(None)) {
          case Left(ParseError("", "[rank]", _)) =>
        }
        TestUtils.inside(ParseUtils.parse_rank(Some("rabbit"))) {
          case Left(ParseError("", "[rank]", _)) =>
        }
      }
      "parse_string_offset" - {
        TestUtils.inside(ParseUtils.parse_string_offset("...HERE...", "HERE", "error")) {
          case Right(3) =>
        }
        TestUtils.inside(ParseUtils.parse_string_offset("...HERE...", "NOT_HERE", "error")) {
          case Left(ParseError("", "[error]", _)) =>
        }
      }
      //(add the error building functions at some point, but not a prio since effectively tested by other suites)
    }
  }
}
