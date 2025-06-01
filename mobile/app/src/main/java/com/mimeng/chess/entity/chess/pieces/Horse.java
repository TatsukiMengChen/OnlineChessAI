package com.mimeng.chess.entity.chess.pieces;

import com.mimeng.chess.entity.chess.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 马
 */
public class Horse extends ChessPiece {

  public Horse(PlayerColor color, Position position) {
    super(PieceType.HORSE, color, position);
  }

  @Override
  public List<Position> getPossibleMoves(ChessBoard board) {
    List<Position> moves = new ArrayList<>();

    // 马走日字，8个可能的位置
    int[] dx = { -2, -2, -1, -1, 1, 1, 2, 2 };
    int[] dy = { -1, 1, -2, 2, -2, 2, -1, 1 };

    for (int i = 0; i < 8; i++) {
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

    int rowDiff = target.getRow() - position.getRow();
    int colDiff = target.getCol() - position.getCol();

    // 检查是否是日字移动
    boolean isValidMove = (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 1) ||
        (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 2);

    if (!isValidMove) {
      return false;
    }

    // 检查马脚是否被卡住
    Position legPosition;
    if (Math.abs(rowDiff) == 2) {
      // 纵向移动两格，检查中间一格
      legPosition = new Position(position.getRow() + rowDiff / 2, position.getCol());
    } else {
      // 横向移动两格，检查中间一格
      legPosition = new Position(position.getRow(), position.getCol() + colDiff / 2);
    }

    return board.getPieceAt(legPosition) == null;
  }
}
