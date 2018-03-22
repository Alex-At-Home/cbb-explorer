package org.piggottfamily.cbb_explorer.utils.parsers

/**
 * Contains the results of a successfully or partially successful parse
 */
case class ParseResponse[T](response: T, warnings: List[ParseError] = Nil)
