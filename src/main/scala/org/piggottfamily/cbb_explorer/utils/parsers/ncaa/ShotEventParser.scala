package org.piggottfamily.cbb_explorer.utils.parsers.ncaa

import org.piggottfamily.cbb_explorer.models._
import org.piggottfamily.cbb_explorer.models.ncaa._
import org.piggottfamily.cbb_explorer.utils.parsers._
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._
import cats.implicits._
import cats.data._
import com.github.dwickern.macros.NameOf._
import shapeless._
import ops.hlist._
import shapeless.labelled._
import record._
import ops.record._
import syntax.singleton._
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Try

/** Parses the game HTML (or game subsets of the team HTML) */
trait ShotEventParser {

  import ExtractorUtils._
  import LineupUtils._

  // Useful documentation for JSoup:
  // https://jsoup.org/cookbook/extracting-data/selector-syntax

  protected val `ncaa.parse_shotevent` = "ncaa.parse_shotevent"

  // Holds all the HTML parsing logic
  protected trait base_builders {
    def team_finder(doc: Document): List[String]
    def shot_event_finder(doc: Document): List[Element]
    def script_extractor(doc: Document): Option[String]
    def title_extractor(event: Element): Option[String]

    def event_period_finder(event: Element): Option[Int]
    def event_time_finder(event: Element): Option[Double]
    def event_player_finder(event: Element): Option[String]
    def shot_location_finder(event: Element): Option[(Double, Double)]
    def event_score_finder(event: Element): Option[Game.Score]
    def shot_result_finder(event: Element): Option[Boolean]
    def shot_taking_team_finder(event: Element): Option[String]
  }

  protected object v1_builders extends base_builders {
    def team_finder(doc: Document): List[String] =
      (doc >?> elementList("table[align=center] img[alt]"))
        .getOrElse(Nil)
        .map(_.attr("alt"))

    def shot_event_finder(doc: Document): List[Element] =
      (doc >?> elementList("circle.shot"))
        .filter(_.nonEmpty)
        .getOrElse(Nil)

    def script_extractor(doc: Document): Option[String] =
      (doc >?> elementList("script")) match {
        case Some(scripts) =>
          Some(
            scripts
              .map(_.outerHtml)
              .filter(_.take(128).contains("addShot("))
              .mkString("\n")
          ).filter(_.nonEmpty)
        case _ => None
      }

    def title_extractor(event: Element): Option[String] =
      (event >?> elementList("title")).getOrElse(Nil).headOption.map(_.text)

    private val period_regex =
      "([0-9]+)(?:st|nd|rd|th) [0-9]+:[0-9]+:[0-9]+.*".r
    def event_period_finder(event: Element): Option[Int] =
      title_extractor(event) match {
        case Some(period_regex(period)) => Try(period.toInt).toOption
        case _                          => None
      }

    private val time_regex = ".*?([0-9]+):([0-9]+)(?:[:]([0-9]+))? .*".r
    def event_time_finder(event: Element): Option[Double] =
      title_extractor(event) match {
        case Some(time_regex(min, sec, maybe_csec)) =>
          Try(
            min.toInt + sec.toInt / 60.0 + Option(maybe_csec)
              .map(_.toInt)
              .getOrElse(0) / 6000.0
          ).toOption
        case _ => None
      }

    private def resolve_team_name(
        name_and_team: String
    ): Option[(String, String)] = {
      val (result, _) = name_and_team.reverse.foldLeft(("", 0)) {
        case ((acc, count), char) =>
          char match {
            // Found team, keep going until end
            case _ if count == -1 => (acc, count)
            // Bracket processing
            case ')' if count == 0 => (acc, count + 1)
            case ')'               => (char +: acc, count + 1)
            case '(' if count == 1 => (acc, -1)
            case '('               => (char +: acc, count - 1)
            // Char processing (if count == 0 we didn't open with a bracket so no team)
            case _ if count == 0 => (acc, -1)
            case _               => (char +: acc, count)
          }
      }
      if (result.isEmpty) None
      else
        Some(
          name_and_team.dropRight(result.length + 2),
          result
        )
    }

    // eg 1st 17:25:00 : made by $PLAYER_STRING($TEAM) 4-5
    private val player_and_team_regex =
      ".*?(?:made|missed) by *?(.*?) [0-9]+-[0-9]+.*".r
    def event_player_finder(event: Element): Option[String] =
      title_extractor(event) match {
        case Some(player_and_team_regex(name_and_team)) =>
          resolve_team_name(name_and_team)
            .map(_._1)
            .map(_.trim)
            .map(ExtractorUtils.name_in_v0_box_format)
            .map(_.trim)
            .filter(_.nonEmpty)
        case _ => None
      }

    def shot_location_finder(event: Element): Option[(Double, Double)] =
      for {
        x_str <- Try(event.attr("cx")).toOption
        y_str <- Try(event.attr("cy")).toOption
        x <- Try(x_str.toDouble).toOption
        y <- Try(y_str.toDouble).toOption
      } yield (x, y)

    val score_regex = ".* ([0-9]+)[-]([0-9]+)$".r
    def event_score_finder(event: Element): Option[Game.Score] =
      title_extractor(event) match {
        case Some(score_regex(team, oppo)) =>
          Try(Game.Score(team.toInt, oppo.toInt)).toOption
        case _ => None
      }

    private val made_or_missed_regex = ".*?: (made|missed) by.*".r
    def shot_result_finder(event: Element): Option[Boolean] =
      title_extractor(event) match {
        case Some(made_or_missed_regex("made"))   => Some(true)
        case Some(made_or_missed_regex("missed")) => Some(false)
        case _                                    => None
      }

    def shot_taking_team_finder(event: Element): Option[String] =
      title_extractor(event) match {
        case Some(player_and_team_regex(player_and_team)) =>
          resolve_team_name(player_and_team)
            .map(_._2)
            .map(_.trim)
            .filter(_.nonEmpty)
        case _ => None
      }
  }

  /** Combines the different methods to build a set of lineup events */
  def create_shot_event_data(
      filename: String,
      in: String,
      box_lineup: LineupEvent
  ): Either[List[ParseError], List[ShotEvent]] = {
    val player_codes = box_lineup.players.map(_.code).toSet
    val builders = v1_builders

    val doc_request_builder =
      ParseUtils.build_request[Document](`ncaa.parse_shotevent`, filename) _
    val single_error_completer =
      ParseUtils.enrich_sub_error(`ncaa.parse_shotevent`, filename) _
    val browser = JsoupBrowser()

    val tidy_ctx =
      LineupErrorAnalysisUtils.build_tidy_player_context(box_lineup)

    // DEBUG
    val debug_print = false

    for {
      doc <- doc_request_builder(browser.parseString(in))

      team_info <- parse_team_name(
        builders.team_finder(doc),
        box_lineup.team.team,
        box_lineup.team.year
      ).left.map(single_error_completer)

      (_, _, target_team_first) = team_info // SI-5589

      _ = if (debug_print) {
        println(
          s"---------------------------- SHOTS FOR: [${box_lineup.team}] vs [${box_lineup.opponent}]"
        )
      }

      html_events <- builders.shot_event_finder(doc) match {
        case events if events.nonEmpty => Right(events)
        case _                         =>
          // Actually, this isn't unexpected, the page is built on the fly so you need to convert the JS to HTML
          builders.script_extractor(doc).map { js =>
            shot_js_to_html(js, builders, browser)
          } match {
            case Some(events) if events.nonEmpty => Right(events)
            case _ =>
              Left(
                List(
                  ParseUtils.build_sub_error(`ncaa.parse_shotevent`)(
                    s"No shot events found [$doc]"
                  )
                )
              )
          }
      }

      // Phase 1, get as much stuff out as we can based on just the events themselves
      // (phase 2 fills in given the context of all the event data)
      very_raw_events <- html_events.map { event =>
        parse_shot_html(
          event,
          box_lineup,
          builders,
          tidy_ctx,
          target_team_first
        )
      }.sequence

      sorted_very_raw_events = very_raw_events.sortBy { case (period, shot) =>
        period * 1000 - shot.min // (switch to correctly sorted ascending times)
      }

      sorted_raw_events = phase1_shot_event_enrichment(sorted_very_raw_events)

      _ =
        if (debug_print) sorted_raw_events.foreach { shot =>
          println(
            s"[${shot.player.map(_.id).getOrElse(shot.opponent.team)}][${f"${shot.min}%.2f"}] " +
              s"dist=[${f"${shot.dist}%.2f"}] hit?=[${shot.pts}]"
          )
        }

    } yield sorted_raw_events

  }

  /** Turns out the page is built on the fly, I already wrote all the code to
    * parse the generated HTML, so I'll just convert the JS to HTML and parse
    * that using the code I already wrote, even though it's slightly wasteful,
    * (Oops!)
    */
  protected def shot_js_to_html(
      js: String,
      builders: base_builders,
      browser: Browser
  ): List[Element] = {
    val js_regex =
      """ *addShot[(]([^,]+), *([^,]+), *([^,]+), *([^,]+), *([^,]+), *'([^']+)',.*""".r
    val html = js
      .split("\n")
      .collect { case js_regex(x, y, _, _, _, title) =>
        val cx = 0.01 * x.toDouble * ShotMapDimensions.court_length_x_px
        val cy = 0.01 * y.toDouble * ShotMapDimensions.court_width_y_px
        s"""<circle class="shot" cx="$cx" cy="$cy" r="5"><title>$title<title/></circle>"""
      }
      .mkString("\n")

    v1_builders.shot_event_finder(browser.parseString(html))
  }

  /** An initial parse of the shot HTML based solely on the HTML itself - some
    * fields cannot be filled in until we have more events to generate context
    */
  protected def parse_shot_html(
      event: Element,
      box_lineup: LineupEvent,
      builders: base_builders,
      tidy_ctx: LineupErrorAnalysisUtils.TidyPlayerContext,
      target_team_first: Boolean
  ): Either[List[ParseError], (Int, ShotEvent)] = {
    val field_tuples = (
      builders.event_period_finder(event),
      builders.event_time_finder(event),
      builders.event_player_finder(event),
      builders.shot_location_finder(event),
      builders.event_score_finder(event),
      builders.shot_result_finder(event),
      builders.shot_taking_team_finder(event)
    )
    field_tuples.mapN((_, _, _, _, _, _, _)) match {
      case Some(
            (
              period,
              time,
              raw_player,
              location,
              score,
              result,
              raw_shot_taking_team
            )
          ) =>
        val player = ExtractorUtils.remove_html_encoding(raw_player)
        val shot_taking_team =
          ExtractorUtils.remove_html_encoding(raw_shot_taking_team)
        val is_offensive = box_lineup.team.team.name == shot_taking_team

        val maybe_player_code_id = if (is_offensive) {
          val (tidier_player_name, _) =
            LineupErrorAnalysisUtils.tidy_player(player, tidy_ctx)

          Some(
            ExtractorUtils.build_player_code(
              tidier_player_name,
              Some(box_lineup.team.team)
            )
          )

        } else {
          // We still extract the player name for the opponent, to help correlate with the PbP data
          // (but we care less about the accuracy of this since it's a fallback anyway)
          Some(
            ExtractorUtils.build_player_code(player, team = None)
          )
        }

        Right(
          period -> build_base_event(box_lineup).copy(
            player = maybe_player_code_id,
            is_off = is_offensive,
            score = box_lineup.location_type match {
              case Game.LocationType.Home => score
              case Game.LocationType.Away =>
                Game.Score(score.allowed, score.scored)
              case Game.LocationType.Neutral =>
                if (target_team_first) score
                else Game.Score(score.allowed, score.scored)
            },
            min = time,
            raw_event = builders.title_extractor(event),
            loc = ShotEvent.ShotLocation(
              x = location._1, // (enrich these in next phase of this function)
              y = location._2
            ),
            pts =
              if (result) 1
              else 0 // (enrich in final phase)
          )
        )
      case _ =>
        val missing_params =
          field_tuples.productIterator.zipWithIndex.collect {
            case (None, idx) => idx
          }
        Left(
          List(
            ParseUtils.build_sub_error(`ncaa.parse_shotevent`)(
              s"Missing fields from shot: param_indices=" +
                s"[${missing_params.mkString(",")}] in [${event.outerHtml}]"
            )
          )
        )
    }
  }

  /** Quick util to fill in some basic fields for the lineup event */
  protected def build_base_event(
      box_lineup: LineupEvent
  ): ShotEvent = {
    ShotEvent(
      player = None, // (override immediately)
      date = box_lineup.date,
      location_type = box_lineup.location_type,
      team = box_lineup.team,
      opponent = box_lineup.opponent,
      is_off = true, // (override immediately)
      lineup_id = None, // (fill in final phase)
      players = Nil, // (fill in later)
      score = Game.Score(0, 0), // (override immediately)
      min = 0.0, // (override immediately)
      raw_event = None, // (fill in later)
      loc = ShotEvent.ShotLocation(
        0.0,
        0.0
      ), // (override immediately; enrich these in next phase of this function)
      geo = ShotEvent.ShotGeo(
        0.0,
        0.0
      ), // (fill in right at the end)
      dist = 0.0, // (fill in these in next phase of this function)
      pts = 0, // (override immediately)
      value = 0, // (fill in final phase)
      ast_by = None, // (fill these in final phase)
      is_ast = None,
      is_trans = None
    )
  }

  /** Now we have a collection of events, labelled with period, we can fill in
    * some more fields
    */
  protected def phase1_shot_event_enrichment(
      sorted_very_raw_events: List[(Int, ShotEvent)],
      second_half_override: Option[Set[Int]] = None
  ): List[ShotEvent] = {
    val LONG_DISTANCE = 50.0
    val women_game = is_women_game(sorted_very_raw_events)

    // Next question ... which side of the screen is which team shooting on
    val (team_shooting_left_in_first_period, first_period) =
      is_team_shooting_left_to_start(sorted_very_raw_events)

    case class State(
        total_shots: Map[Int, Int] = Map(),
        long_shots: Map[Int, Int] = Map(),
        shots: List[ShotEvent] = Nil
    )
    val results = sorted_very_raw_events.foldLeft(State()) {
      case (state, (period, shot)) =>
        val ascending_time = get_ascending_time(shot, period, women_game)
        val second_half_switch = if (women_game) {
          period > 2 && first_period <= 2 // (just handle edge case where first shot is in 2nd half)
        } else {
          period > 1 && first_period <= 1
        }

        val (x, y, alt_x, alt_y) = transform_shot_location(
          shot.loc.x,
          shot.loc.y,
          second_half_override.exists(
            _.contains(period)
          ) match {
            case true => !second_half_switch
            case _    => second_half_switch
          },
          team_shooting_left_in_first_period,
          shot.is_off
        )
        val dist = Math.sqrt(x * x + y * y)
        val alt_dist = Math.sqrt(alt_x * alt_x + alt_y * alt_y)
        val trans_shot =
          if ((dist < 1.2 * alt_dist) || (shot.min < 0.1)) {
            // (the 1.2x means if the 2 are close we trust the original more)
            // (currently the shot clock is descending so this is "shots with <6s left in the quarter")
            shot.copy(
              loc = ShotEvent.ShotLocation(x, y),
              dist = dist,
              min = ascending_time
            )
          } else {
            shot.copy(
              loc = ShotEvent.ShotLocation(alt_x, alt_y),
              dist = alt_dist,
              min = ascending_time
            )
          }

        // Create a fake geo for the points (converting ft to meters to give our 64b floats a better
        // resolution. We do this because there's lots more visualizations and aggregations for geo)
        val BASE_LAT = 40.750298
        val BASE_LON = -73.993324
        val `PI/180` = Math.PI / 180.0
        val `180/PI` = 1.0 / `PI/180`
        val EARTH_RADIUS = 6371000.0
        val shot_lat = BASE_LAT + (trans_shot.loc.y / EARTH_RADIUS) * `180/PI`
        val eff_radius = EARTH_RADIUS * Math.cos(shot_lat * `PI/180`)
        val shot_lon = BASE_LON + (trans_shot.loc.x / eff_radius) * `180/PI`

        val trans_shot_with_geo = trans_shot.copy(
          geo = ShotEvent.ShotGeo(
            lat = shot_lat,
            lon = shot_lon
          )
        )

        // (note: we use "dist" to look for long shots, because that way we can look for systemtically
        // bad periods where the court is flipped)
        state.copy(
          state.total_shots + (period -> (state.total_shots
            .getOrElse(period, 0) + 1)),
          if (dist > LONG_DISTANCE)
            state.long_shots + (period -> (state.long_shots
              .getOrElse(period, 0) + 1))
          else state.long_shots,
          shots = state.shots ++ List(trans_shot_with_geo)
        )
    }
    val maybe_problem_periods = results.long_shots.filter {
      case (period, count) =>
        val total_shots = results.total_shots.getOrElse(period, 1)
        // if too many shots are long distance, probably the court is flipped
        val bad_period = total_shots >= 6 && count > 0.75 * total_shots
        bad_period
    }.keys

    if (maybe_problem_periods.nonEmpty) {
      val firstShot = sorted_very_raw_events.headOption
      val team = firstShot.map(_._2.team.team)
      val oppo = firstShot.map(_._2.opponent.team)
      println(
        s"[p1_s_e_e] [WARNING] [$team]v[$oppo] Flip court for periods: [$maybe_problem_periods] because [${results
            .copy(shots = Nil)}]"
      )
    }

    // If we had problem periods override them and re-run:
    if (maybe_problem_periods.nonEmpty && second_half_override.isEmpty) {
      phase1_shot_event_enrichment(
        sorted_very_raw_events,
        Some(maybe_problem_periods.toSet)
      )
    } else {
      results.shots
    }
  }

  /** Converts the descending time within a period to an ascending game time */
  protected def get_ascending_time(
      event: ShotEvent,
      period: Int,
      is_women_game: Boolean
  ): Double = {
    ExtractorUtils.duration_from_period(period, is_women_game) - event.min
  }

  /** It seems random which team gets the left court on the shot graphics */
  protected def is_team_shooting_left_to_start(
      sorted_very_raw_events: List[(Int, ShotEvent)]
  ): (Boolean, Int) = {
    val first_period =
      sorted_very_raw_events.headOption.map(_._1).getOrElse(1)
    val first_period_shots =
      sorted_very_raw_events.takeWhile(_._1 == first_period).map(_._2)
    val (shots_to_left, shots_to_right) =
      first_period_shots.filter(_.is_off).partition {
        _.loc.x < ShotMapDimensions.half_court_x_px
      }
    val team_shooting_left_in_first_period =
      shots_to_left.size > shots_to_right.size

    (team_shooting_left_in_first_period, first_period)
  }

  /** Infers if the game is men or women based on timing events */
  protected def is_women_game(
      sorted_very_raw_events: List[(Int, ShotEvent)]
  ): Boolean = {
    val num_periods = sorted_very_raw_events.lastOption.map(_._1).getOrElse(2)
    // Note that time is descending, so > 10 mins means eg 11:00:00 on the clock
    val shot_taken_before_1st_quarter_starts =
      sorted_very_raw_events.headOption.exists { _._2.min > 10.0 }
    (num_periods >= 4) && !shot_taken_before_1st_quarter_starts
  }

  /** Shot dimensions taken from svg#court, should consider extracting these */
  object ShotMapDimensions {
    val court_length_x_px = 940.0
    val court_width_y_px = 500.0
    val court_length_ft = 94.0
    val court_width_ft = 50.0

    val half_court_x_px = 0.5 * court_length_x_px

    val ft_per_px_x = court_length_ft / court_length_x_px
    val ft_per_px_y = court_width_ft / court_width_y_px

    val goal_left_x_px = 50.0
    val goal_y_px = 250.0
  }

  /** Transform from pixel on screen to feet vs bucket -
    * @returns
    *   x/y it believes correct; the alternative x,y
    */
  protected def transform_shot_location(
      x: Double,
      y: Double,
      second_half_switch: Boolean,
      team_shooting_left_in_first_period: Boolean,
      is_offensive: Boolean
  ): (Double, Double, Double, Double) = {

    // Step 1: transform to always be on the left side of the court
    // (each 'false' switches the side of the court)
    val goal_is_to_left = Array(
      team_shooting_left_in_first_period,
      !second_half_switch,
      is_offensive
    ).map(if (_) 1 else -1).reduce(_ * _) > 0

    val alt_x = ShotMapDimensions.court_length_x_px - x
    val alt_y = ShotMapDimensions.court_width_y_px - y
    val (trans_x, trans_y, alt_trans_x, alt_trans_y) = if (goal_is_to_left) {
      (x, y, alt_x, alt_y)
    } else { // (we switch left so that left/right side of basket is consistent)
      (alt_x, alt_y, x, y)
    }

    // Step 2: co-ords relative to goal

    (
      (trans_x - ShotMapDimensions.goal_left_x_px) * ShotMapDimensions.ft_per_px_x,
      (ShotMapDimensions.goal_y_px - trans_y) * ShotMapDimensions.ft_per_px_y, // (+ve is to the right)
      (alt_trans_x - ShotMapDimensions.goal_left_x_px) * ShotMapDimensions.ft_per_px_x,
      (ShotMapDimensions.goal_y_px - alt_trans_y) * ShotMapDimensions.ft_per_px_y // (+ve is to the right)
    )
  }

}
object ShotEventParser extends ShotEventParser
