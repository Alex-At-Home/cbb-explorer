package org.piggottfamily.cbb_explorer.utils.parsers

import scala.util.{Try, Success, Failure}
import com.github.dwickern.macros.NameOf._
import shapeless._
import ops.hlist._
import  org.piggottfamily.cbb_explorer.models.Metric
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._

/** Utilities for managing generation and accumulation of errors */
object ParseUtils {

  // Standard parsing

  /** Parses out a score (double) from HTML */
  def parse_score(el: Option[String]): Either[ParseError, Double] = el match {
    case Some(score) =>
      ParseUtils.build_sub_request[Double](nameOf[Metric](_.value))(score.toDouble)
    case None =>
      Left(ParseUtils.build_sub_error(nameOf[Metric](_.value))(
        s"Failed to locate the field"
      ))
  }

  /** Parses out a rank from HTML */
  def parse_rank(el: Option[String]): Either[ParseError, Int] = el match {
    case Some(rank) =>
      ParseUtils.build_sub_request[Int](nameOf[Metric](_.rank))(rank.toInt)
    case None =>
      Left(ParseUtils.build_sub_error(nameOf[Metric](_.rank))(
        s"Failed to locate the field"
      ))
  }

  /** Utility for buisub_str1lding sub-strings */
  def parse_string_offset(in: String, token: String, token_type: String):
    Either[ParseError, Int] =
      for {
        index <- Right(in.indexOf(token)).flatMap {
          case valid if valid >= 0 => Right(valid)
          case invalid =>
            Left(ParseUtils.build_sub_error(token_type)(
              s"Failed to locate [$token] at [$token_type] in [$in]"
            ))
        }
      } yield index

  // Error creation

  /** Puts ids in []s for readability */
  def build_error_id(value: String) = if (value.nonEmpty) s"[$value]" else ""

  /** Builds an either from a request that might throw, returns a list for consistency */
  def build_request[T](location: String, base_id: String, subids: String*)(request: => T): Either[List[ParseError], T] = Try(request) match {
    case Success(x) => Right(x)
    case Failure(e) => Left(List(ParseError(location, (base_id :: subids.toList).map(build_error_id(_)).mkString(""), s"Exception=[${e.getMessage}]")))
  }

  /** Builds an either from a request that might throw, returns a list for consistency */
  def build_sub_request[T](subids: String*)(request: => T): Either[ParseError, T] =
    build_request[T]("", "", subids:_*)(request).left.map(_.head)

  /** Builds an error with multiple error strings */
  def build_errors(location: String, base_id: String, subids: String*)(errors: List[String]): ParseError = {
    ParseError(location, (base_id :: subids.toList).map(build_error_id(_)).mkString(""), errors)
  }
  /** Builds an error with a single error strings */
  def build_error(location: String, base_id: String, subids: String*)(error: String): ParseError = {
    build_errors(location, base_id, subids:_*)(List(error))
  }

  /** Builds a list of errors with no location info - should be enriched via enrich_error(s) */
  def build_sub_errors(subids: String*)(errors: List[String]): ParseError = {
    build_errors("", "", subids:_*)(errors)
  }
  /** Builds a single error with no location info - should be enriched via enrich_error(s) */
  def build_sub_error(subids: String*)(error: String): ParseError = {
    build_error("", "", subids:_*)(error)
  }
  /** Adds top-level location information to a list of sub errors generated by a child */
  def enrich_sub_errors(location: String, base_id: String)(errors: List[ParseError]): List[ParseError] = {
    errors.map(error => ParseError(location, build_error_id(base_id) + error.id, error.messages))
  }
  /** Adds top-level location information to a sub error generated by a child, returns a list for consistency */
  def enrich_sub_error(location: String, base_id: String)(error: ParseError): List[ParseError] = {
    enrich_sub_errors(location, base_id)(List(error))
  }

  // Shapeless error accumulation

  private type EitherError[T] = Either[ParseError, T]
  private type EitherMultiError[T] = Either[List[ParseError], T]

  object right_only_kv extends Poly1 {
    private def handler[T](x: Either[_, T]): T = x match {
      case Right(t) => t
      case _        => throw new Exception("Internal Logic Error")
    }
    implicit def either[K, T] =
      at[FieldType[K, EitherError[T]]](kv => field[K](handler[T](kv)))
    implicit def either_multi[K, T] =
      at[FieldType[K, EitherMultiError[T]]](kv => field[K](handler[T](kv)))
  }
  object left_or_filter_right_kv extends Poly1 {
    implicit def either[K, T] = at[FieldType[K, EitherError[T]]](kv => {
      val either_val: EitherError[T] = kv
      either_val match {
        case Right(_) => Nil
        case Left(s)  => List(s)
      }
    })
    implicit def either_multi[K, T] = at[FieldType[K, EitherMultiError[T]]](kv => {
      val either_val: EitherMultiError[T] = kv
      either_val match {
        case Right(_) => Nil
        case Left(ls)  => ls
      }
    })
  }

  /** Sequences an HList of Either[ParseError, _] or Either[List[ParseError], _]
      and returns either an HList of the boxed values or an aggregation of all
      the ParseErrors
  */
  def sequence_kv_results[I <: HList, R <: HList, M <: HList](in: I)(
      implicit
      right_only_mapper: Mapper.Aux[right_only_kv.type, I, R],
      left_or_filter_right_mapper: Mapper.Aux[left_or_filter_right_kv.type, I, M],
      to_list: ToTraversable.Aux[M, List, List[ParseError]])
    : Either[List[ParseError], R] =
  {
    (in map left_or_filter_right_kv).toList.flatten match {
      case Nil => Right(in map right_only_kv)
      case l   => Left(l)
    }
  }
}
