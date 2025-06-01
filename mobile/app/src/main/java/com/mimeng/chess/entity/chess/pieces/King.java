package com.mimeng.chess.entity.chess.pieces;

import com.mimeng.chess.entity.chess.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 将/帅
 */
public class King extends ChessPiece {

  public King(PlayerColor color, Position position) {
    super(PieceType.KING, color, position);
  }

  @Override
  public List<Position> getPossibleMoves(ChessBoard board) {
    List<Position> moves = new ArrayList<>();

    // 将/帅只能在九宫格内移动，且只能走一格
    int[] dx = { -1, 1, 0, 0 };
    int[] dy = { 0, 0, -1, 1 };

    for (int i = 0; i < 4; i++) {
      Position newPos = position.move(dx[i], dy[i]);
      if (canMoveTo(newPos, board)) {
        moves.add(newPos);
      }
    }

    return moves;
  }

  @Override
  public boolean canMoveTo(Position target, ChessBoard board) {
    if (!isValidTarget(target, board)) {
      return false;
    }

    // 必须在九宫格内
    boolean inPalace = (color == PlayerColor.RED && target.isInRedPalace()) ||
        (color == PlayerColor.BLACK && target.isInBlackPalace());
    if (!inPalace) {
      return false;
    }

    // 只能移动一格，且只能水平或垂直移动
    int rowDiff = Math.abs(target.getRow() - position.getRow());
    int colDiff = Math.abs(target.getCol() - position.getCol());

    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
  }
}
