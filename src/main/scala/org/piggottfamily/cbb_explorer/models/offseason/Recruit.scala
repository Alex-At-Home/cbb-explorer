package org.piggottfamily.cbb_explorer.models.offseason


final case class Recruit(name: String, pos: String, height_in: Int, rating: Double, team: String, profile: Option[String])