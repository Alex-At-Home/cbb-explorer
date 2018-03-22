package org.piggottfamily.cbb_explorer.models

/**
 * Represents a CBB season
 * @param value The year in which the season _ends_
 */
case class Year(value: Int) extends AnyVal {
	def until(including: Year): List[Year] = {
		new Range(value, including.value, 1).toList.map(Year(_))
	}
}
object Year {

	val y2002_ = Year(2003)
	val y2003_ = Year(2004)
	val y2004_ = Year(2005)
	val y2005_ = Year(2006)
	val y2006_ = Year(2007)
	val y2007_ = Year(2008)
	val y2008_ = Year(2009)
	val y2009_ = Year(2010)
	val y2010_ = Year(2011)
	val y2011_ = Year(2012)
	val y2012_ = Year(2013)
	val y2013_ = Year(2014)
	val y2014_ = Year(2015)
	val y2015_ = Year(2016)
	val y2016_ = Year(2017)
	val y2017_ = Year(2018)
	val y2018_ = Year(2019)

	val y_2002 = Year(2002)
	val y_2003 = Year(2003)
	val y_2004 = Year(2004)
	val y_2005 = Year(2005)
	val y_2006 = Year(2006)
	val y_2007 = Year(2007)
	val y_2008 = Year(2008)
	val y_2009 = Year(2009)
	val y_2010 = Year(2010)
	val y_2011 = Year(2011)
	val y_2012 = Year(2012)
	val y_2013 = Year(2013)
	val y_2014 = Year(2014)
	val y_2015 = Year(2015)
	val y_2016 = Year(2016)
	val y_2017 = Year(2017)
	val y_2018 = Year(2018)
}
