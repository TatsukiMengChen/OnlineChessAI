package com.mimeng.chess.entity.chess.pieces;

import com.mimeng.chess.entity.chess.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 仕/士
 */
public class Guard extends ChessPiece {

  public Guard(PlayerColor color, Position position) {
    super(PieceType.GUARD, color, position);
  }

  @Override
  public List<Position> getPossibleMoves(ChessBoard board) {
    List<Position> moves = new ArrayList<>();

    // 仕只能斜向移动一格，且必须在九宫格内
    int[] dx = { -1, -1, 1, 1 };
    int[] dy = { -1, 1, -1, 1 };

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

    // 只能斜向移动一格
    int rowDiff = Math.abs(target.getRow() - position.getRow());
    int colDiff = Math.abs(target.getCol() - position.getCol());

    return rowDiff == 1 && colDiff == 1;
  }
}
