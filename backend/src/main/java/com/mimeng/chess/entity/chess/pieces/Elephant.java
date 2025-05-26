package com.mimeng.chess.entity.chess.pieces;

import com.mimeng.chess.entity.chess.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 象/相
 */
public class Elephant extends ChessPiece {

  public Elephant(PlayerColor color, Position position) {
    super(PieceType.ELEPHANT, color, position);
  }

  @Override
  public List<Position> getPossibleMoves(ChessBoard board) {
    List<Position> moves = new ArrayList<>();

    // 象走田字，4个可能的位置
    int[] dx = { -2, -2, 2, 2 };
    int[] dy = { -2, 2, -2, 2 };

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

    // 象不能过河
    boolean sameHalf = (color == PlayerColor.RED && target.isInRedSide()) ||
        (color == PlayerColor.BLACK && target.isInBlackSide());
    if (!sameHalf) {
      return false;
    }

    int rowDiff = target.getRow() - position.getRow();
    int colDiff = target.getCol() - position.getCol();

    // 必须是田字移动
    if (Math.abs(rowDiff) != 2 || Math.abs(colDiff) != 2) {
      return false;
    }

    // 检查象眼是否被卡住
    Position eyePosition = new Position(
        position.getRow() + rowDiff / 2,
        position.getCol() + colDiff / 2);

    return board.getPieceAt(eyePosition) == null;
  }
}
