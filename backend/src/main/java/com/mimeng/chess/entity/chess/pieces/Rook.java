package com.mimeng.chess.entity.chess.pieces;

import com.mimeng.chess.entity.chess.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 车
 */
public class Rook extends ChessPiece {

  public Rook(PlayerColor color, Position position) {
    super(PieceType.ROOK, color, position);
  }

  @Override
  public List<Position> getPossibleMoves(ChessBoard board) {
    List<Position> moves = new ArrayList<>();

    // 车可以沿直线移动任意距离
    int[] dx = { -1, 1, 0, 0 };
    int[] dy = { 0, 0, -1, 1 };

    for (int dir = 0; dir < 4; dir++) {
      for (int step = 1; step <= 10; step++) {
        Position newPos = position.move(dx[dir] * step, dy[dir] * step);

        if (!newPos.isValid()) {
          break;
        }

        ChessPiece pieceAtTarget = board.getPieceAt(newPos);

        if (pieceAtTarget == null) {
          moves.add(newPos);
        } else {
          // 遇到棋子
          if (pieceAtTarget.getColor() != this.color) {
            moves.add(newPos); // 可以吃掉敌方棋子
          }
          break; // 无论如何都不能继续前进
        }
      }
    }

    return moves;
  }

  @Override
  public boolean canMoveTo(Position target, ChessBoard board) {
    if (!isValidTarget(target, board)) {
      return false;
    }

    // 必须在同一行或同一列
    if (target.getRow() != position.getRow() && target.getCol() != position.getCol()) {
      return false;
    }

    // 检查路径是否畅通
    return isPathClear(position, target, board);
  }
}
