package com.mimeng.chess.entity.chess;

import java.util.ArrayList;
import java.util.List;

/**
 * 象棋游戏逻辑类 - 负责胜负判断和游戏规则检查
 */
public class GameLogic {

  /**
   * 检查是否将军
   */
  public static boolean isInCheck(ChessBoard board, PlayerColor color) {
    ChessPiece king = board.getKing(color);
    if (king == null) {
      return false;
    }

    PlayerColor opponentColor = (color == PlayerColor.RED) ? PlayerColor.BLACK : PlayerColor.RED;
    List<ChessPiece> opponentPieces = board.getPieces(opponentColor);

    for (ChessPiece piece : opponentPieces) {
      if (piece.canMoveTo(king.getPosition(), board)) {
        return true;
      }
    }

    return false;
  }

  /**
   * 检查是否将死
   */
  public static boolean isCheckmate(ChessBoard board, PlayerColor color) {
    if (!isInCheck(board, color)) {
      return false;
    }

    // 尝试所有可能的移动，看是否能解除将军
    List<Move> possibleMoves = board.getAllPossibleMoves(color);

    for (Move move : possibleMoves) {
      // 临时执行移动
      ChessBoard tempBoard = board.copy();
      tempBoard.makeMove(move.getFrom(), move.getTo());

      // 检查移动后是否仍然被将军
      if (!isInCheck(tempBoard, color)) {
        return false; // 找到了解除将军的方法
      }
    }

    return true; // 没有任何移动能解除将军
  }

  /**
   * 检查是否困毙（无子可动但未被将军）
   */
  public static boolean isStalemate(ChessBoard board, PlayerColor color) {
    if (isInCheck(board, color)) {
      return false;
    }

    List<Move> possibleMoves = board.getAllPossibleMoves(color);

    for (Move move : possibleMoves) {
      ChessBoard tempBoard = board.copy();
      tempBoard.makeMove(move.getFrom(), move.getTo());

      if (!isInCheck(tempBoard, color)) {
        return false; // 还有合法移动
      }
    }

    return true; // 没有合法移动
  }

  /**
   * 检查移动是否合法（不会导致自己被将军）
   */
  public static boolean isLegalMove(ChessBoard board, Move move) {
    ChessPiece piece = board.getPieceAt(move.getFrom());
    if (piece == null) {
      return false;
    }

    // 检查棋子本身是否可以移动到目标位置
    if (!piece.canMoveTo(move.getTo(), board)) {
      return false;
    }

    // 临时执行移动
    ChessBoard tempBoard = board.copy();
    tempBoard.makeMove(move.getFrom(), move.getTo());

    // 检查移动后是否会导致自己被将军
    return !isInCheck(tempBoard, piece.getColor());
  }

  /**
   * 获取所有合法移动
   */
  public static List<Move> getLegalMoves(ChessBoard board, PlayerColor color) {
    List<Move> allMoves = board.getAllPossibleMoves(color);
    List<Move> legalMoves = new ArrayList<>();

    for (Move move : allMoves) {
      if (isLegalMove(board, move)) {
        legalMoves.add(move);
      }
    }

    return legalMoves;
  }

  /**
   * 检查游戏是否结束
   */
  public static GameStatus checkGameStatus(ChessBoard board, PlayerColor currentPlayer) {
    if (isCheckmate(board, currentPlayer)) {
      return (currentPlayer == PlayerColor.RED) ? GameStatus.BLACK_WIN : GameStatus.RED_WIN;
    }

    if (isStalemate(board, currentPlayer)) {
      return GameStatus.DRAW;
    }

    // 检查是否只剩下将帅对峙
    if (isKingVsKingOnly(board)) {
      return GameStatus.DRAW;
    }

    return GameStatus.PLAYING;
  }

  /**
   * 检查是否只剩下将帅对峙
   */
  private static boolean isKingVsKingOnly(ChessBoard board) {
    List<ChessPiece> redPieces = board.getPieces(PlayerColor.RED);
    List<ChessPiece> blackPieces = board.getPieces(PlayerColor.BLACK);

    return redPieces.size() == 1 && blackPieces.size() == 1 &&
        redPieces.get(0).getType() == PieceType.KING &&
        blackPieces.get(0).getType() == PieceType.KING;
  }

  /**
   * 检查将帅是否照面（同一列且中间无子）
   */
  public static boolean areKingsFacing(ChessBoard board) {
    ChessPiece redKing = board.getKing(PlayerColor.RED);
    ChessPiece blackKing = board.getKing(PlayerColor.BLACK);

    if (redKing == null || blackKing == null) {
      return false;
    }

    // 必须在同一列
    if (redKing.getPosition().getCol() != blackKing.getPosition().getCol()) {
      return false;
    }

    // 检查中间是否有其他棋子
    int col = redKing.getPosition().getCol();
    int startRow = Math.min(redKing.getPosition().getRow(), blackKing.getPosition().getRow()) + 1;
    int endRow = Math.max(redKing.getPosition().getRow(), blackKing.getPosition().getRow()) - 1;

    for (int row = startRow; row <= endRow; row++) {
      if (board.getPieceAt(new Position(row, col)) != null) {
        return false;
      }
    }

    return true;
  }
}
