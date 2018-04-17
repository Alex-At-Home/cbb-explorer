package org.piggottfamily.cbb_explorer.utils.parsers

import utest._
import org.piggottfamily.cbb_explorer.utils.TestUtils
import shapeless._
import ops.hlist._
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._

object ParseUtilsTests extends TestSuite {
  private def show_key[K <: Symbol, V](
    kv: FieldType[K, V])(implicit key: Witness.Aux[K]
  ): Symbol = key.value

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
      "sequence_kv_results" - {
        val err1: Either[ParseError, String] = Left(ParseError("", "err1", List()))
        val err2: Either[List[ParseError], Boolean] = Left(List(
          ParseError("", "err2a", List()), ParseError("", "err2b", List())
        ))
        val good1: Either[ParseError, String] = Right("good1")
        val good2: Either[ParseError, Int] = Right(2)

        val mix =
          'f_err1 ->> err1 :: 'f_good1 ->> good1 ::
          'f_err2 ->> err2 :: 'f_good2 ->> good2 ::
          HNil

        val all_good = 'f_good1 ->> good1 :: 'f_good2 ->> good2 :: HNil

        TestUtils.inside(ParseUtils.sequence_kv_results(mix)) {
          case Left(List(
            ParseError("", "err1", List()),
            ParseError("", "err2a", List()),
            ParseError("", "err2b", List())
          )) =>
        }
        TestUtils.inside(ParseUtils.sequence_kv_results(all_good)) {
          // case Right('f_good1 ->> "good1" :: 'f_good2 ->> 2 :: HNil) =>
          case Right(good1_result :: good2_result :: HNil) =>
            show_key(good1_result) ==> 'f_good1
            show_key(good2_result) ==> 'f_good2
            TestUtils.inside((good1, good2)) {
              case (Right(good1_result), Right(good2_result)) =>
            }
        }
      }
      //(add the error building functions at some point, but not a prio since effectively tested by other suites)
    }
  }
}
