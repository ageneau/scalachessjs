package scalachessjs

import scala.scalajs.js.JSApp
import scala.scalajs.js
import js.Dynamic.{ global => g, newInstance => jsnew, literal => jsobj }
import js.JSConverters._
import js.annotation._

import chess.{ Valid, Success, Failure, Board, Game, Color, Pos, Role, PromotableRole, Replay, Status, MoveOrDrop }
import chess.variant.Variant

object Main extends JSApp {
  def main(): Unit = {
  }

  @JSExportNamed
  def getDests(variant: js.UndefOr[String], fen: String): js.Object = {
    val variantOpt: Option[Variant] = variant.asInstanceOf[js.UndefOr[String]].toOption.flatMap(Variant(_));

    val game = Game(variantOpt, Some(fen))
    possibleDests(game).asInstanceOf[js.Object]
  }

  @JSExportNamed
  def move(
    variant: js.UndefOr[String],
    fen: js.UndefOr[String],
    pgnMoves: js.UndefOr[js.Array[String]],
    uciMoves: js.UndefOr[js.Array[String]],
    orig: String,
    dest: String,
    promotion: js.UndefOr[String]): js.Object = {
    val fenOpt : Option[String] = fen.toOption
    val variantOpt = variant.toOption
    val variant_ = variantOpt.flatMap(Variant(_))

    val promotion_ : Option[PromotableRole] = Role.promotable(promotion.toOption)
    val pgnMovesOpt = pgnMoves.toOption
    val uciMovesOpt = uciMoves.toOption
    val pgnMoves_ = pgnMovesOpt.map(_.toList).getOrElse(List.empty[String])
    val uciMoves_ = uciMovesOpt.map(_.toList).getOrElse(List.empty[String])

    (for {
      orig <- Pos.posAt(orig)
      dest <- Pos.posAt(dest)
      fen <- fenOpt
    } yield (orig, dest, fen)) match {
      case Some((orig, dest, fen)) =>
        Game(variant_, fenOpt)(orig, dest, promotion_) match {
          case Success((newGame, move)) => {
            gameToSituationInfo(newGame.withPgnMoves(pgnMoves_ ++ newGame.pgnMoves), uciMoves_, promotion_)
          }
          case Failure(errors) => throw new Exception(errors.head)
        }
      case None =>
        throw new Exception(s"step topic params: $orig, $dest, $fen are not valid")
    }
  }

  @JSExport
  def init(variant: String, fen: String): js.Object = {
    val variantOpt = variant.asInstanceOf[js.UndefOr[String]].toOption.flatMap(Variant(_));

    val game = Game(variantOpt, Some(fen))
    jsobj(
        "variant" -> new VariantInfo {
          val key = game.board.variant.key
          val name = game.board.variant.name
          val shortName = game.board.variant.shortName
          val title = game.board.variant.title
        },
        "setup" -> gameToSituationInfo(game)
    )
  }

  @JSExportNamed
  def threefoldTest(
    variant: String,
    pgnMoves: js.Array[String],
    initialFen: js.UndefOr[String]
  ) : js.Object = {
    val pgnMoves_ = pgnMoves.asInstanceOf[js.Array[String]].toList
    val initialFenOpt = initialFen.asInstanceOf[js.UndefOr[String]].toOption
    val variantOpt = variant.asInstanceOf[js.UndefOr[String]].toOption.flatMap(Variant(_));

    Replay(pgnMoves_, initialFenOpt, variantOpt getOrElse Variant.default) match {
      case Success(replay) => {
        jsobj(
            "threefoldRepetition" -> replay.state.board.history.threefoldRepetition,
            "status" -> jsobj(
              "id" -> Status.Draw.id,
              "name" -> Status.Draw.name
            )
        )
      }
      case Failure(errors) => throw new Exception(errors.head)
    }
  }

  @JSExportNamed
  def pgnRead(
    pgn: String
  ) : js.Object = {
    (for {
      replay <- chess.format.pgn.Reader.full(pgn)
      fen = chess.format.Forsyth >> replay.setup
      games <- replayGames(replay.chronoMoves, Some(fen), replay.setup.board.variant)
    } yield (replay, games)) match {
      case Success((replay, listOfGames)) => {
        jsobj(
          "variant" -> new VariantInfo {
            val key = replay.setup.board.variant.key
            val name = replay.setup.board.variant.name
            val shortName = replay.setup.board.variant.shortName
            val title = replay.setup.board.variant.title
          },
          "setup" -> gameToSituationInfo(replay.setup),
          "replay" -> listOfGames.map(gameToSituationInfo(_)).toJSArray
        )
      }
      case Failure(errors) => throw new Exception(errors.head)
    }
  }

  @JSExportNamed
  def pgnDump(
    variant: js.UndefOr[String],
    pgnMoves: js.Array[String],
    initialFen: js.UndefOr[String],
    white: js.UndefOr[String],
    black: js.UndefOr[String],
    date: js.UndefOr[String]
  ) : js.Object = {
    val pgnMoves_ = pgnMoves.toList
    val initialFenOpt = initialFen.toOption
    val whiteOpt = white.toOption
    val blackOpt = black.toOption
    val dateOpt = date.toOption
    val variantOpt = variant.asInstanceOf[js.UndefOr[String]].toOption.flatMap(Variant(_));

    Replay(pgnMoves_, initialFenOpt, variantOpt getOrElse Variant.default) match {
      case Success(replay) => {
        val pgn = PgnDump(replay.state, initialFenOpt, replay.setup.turns, whiteOpt, blackOpt, dateOpt)
        jsobj(
          "pgn" -> pgn.toString
        )
      }
      case Failure(errors) => throw new Exception(errors.head)
    }
  }

  private def gameToSituationInfo(game: Game, curUciMoves: List[String] = List.empty[String], promotionRole: Option[PromotableRole] = None): js.Object = {
    val movable = !game.situation.end
    val emptyDests: js.Dictionary[js.Array[String]] = js.Dictionary()
    val mergedUciMoves = game.board.history.lastMove.fold(List.empty[String]) { lm =>
      curUciMoves :+ lm.uci
    }

    new SituationInfo {
      val variant = game.board.variant.key
      val fen = chess.format.Forsyth >> game
      val player = game.player.name
      val dests = if (movable) possibleDests(game) else emptyDests
      val end = game.situation.end
      val playable = game.situation.playable(true)
      val winner = game.situation.winner.map(_.name).orUndefined
      val check = game.situation.check
      val checkCount = jsobj(
        "white" -> game.board.history.checkCount.white,
        "black" -> game.board.history.checkCount.black
      )
      val pgnMoves = game.pgnMoves.toJSArray
      val uciMoves = mergedUciMoves.toJSArray
      val promotion = promotionRole.map(_.forsyth).map(_.toString).orUndefined
      val status = game.situation.status.map { s =>
        jsobj(
          "id" -> s.id,
          "name" -> s.name
          )
      }.orUndefined
      val ply = game.turns
    }
  }

  private def possibleDests(game: Game): js.Dictionary[js.Array[String]] = {
    game.situation.destinations.map {
      case (pos, dests) => (pos.toString -> dests.map(_.toString).toJSArray)
    }.toJSDictionary
  }

  private def replayGames(
    moves: List[MoveOrDrop],
    initialFen: Option[String],
    variant: chess.variant.Variant): Valid[List[Game]] = {
      val game = Game(Some(variant), initialFen)
      recursiveGames(game, moves) map { game :: _ }
  }

  private def recursiveGames(game: Game, moves: List[MoveOrDrop]): Valid[List[Game]] =
    moves match {
      case Nil => Success(Nil)
      case moveOrDrop :: rest => {
        val newGame = moveOrDrop.fold(game.apply, game.applyDrop)
        recursiveGames(newGame, rest) map { newGame :: _ }
      }
    }
}

@ScalaJSDefined
trait VariantInfo extends js.Object {
  val key: String
  val name: String
  val shortName: String
  val title: String
}

@ScalaJSDefined
trait SituationInfo extends js.Object {
  val variant: String
  val fen: String
  val player: String
  val dests: js.Dictionary[js.Array[String]]
  val end: Boolean
  val playable: Boolean
  val status: js.UndefOr[js.Object]
  val winner: js.UndefOr[String]
  val check: Boolean
  val checkCount: js.Object
  val pgnMoves: js.Array[String]
  val uciMoves: js.Array[String]
  val promotion: js.UndefOr[String]
  val ply: Int
}
