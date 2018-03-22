package org.piggottfamily.cbb_explorer.utils.parsers

/**
 * Encapsulates a set of errors seen during parsing
 * @param location The module in which the error occurred
 * @param id The module-specific id for which the error occurred
 * @param messages A human readable description of the errors
 */
case class ParseError(location: String, id: String, messages: List[String])

object ParseError {

  /**
   * Encapsulates a single error seen during parsing
   * @param location The module in which the error occurred
   * @param id The module-specific id for which the error occurred
   * @param message A human readable description of the error
   */
  def apply(location: String, id: String, message: String): ParseError = {
    ParseError(location, id, List(message))
  }
}
