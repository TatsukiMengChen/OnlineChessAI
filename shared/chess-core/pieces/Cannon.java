package com.mimeng.chess.entity.chess.pieces;

import com.mimeng.chess.entity.chess.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 炮
 */
public class Cannon extends ChessPiece {

  public Cannon(PlayerColor color, Position position) {
    super(PieceType.CANNON, color, position);
  }

  @Override
  public List<Position> getPossibleMoves(ChessBoard board) {
    List<Position> moves = new ArrayList<>();

    // 炮沿直线移动
    int[] dx = { -1, 1, 0, 0 };
    int[] dy = { 0, 0, -1, 1 };

    for (int dir = 0; dir < 4; dir++) {
      // 不翻山的移动
      for (int step = 1; step <= 10; step++) {
        Position newPos = position.move(dx[dir] * step, dy[dir] * step);

        if (!newPos.isValid()) {
          break;
        }

        ChessPiece pieceAtTarget = board.getPieceAt(newPos);

        if (pieceAtTarget == null) {
          moves.add(newPos);
        } else {
          // 遇到第一个棋子，作为炮台，继续寻找可以吃的目标
          for (int step2 = step + 1; step2 <= 10; step2++) {
            Position cannonTarget = position.move(dx[dir] * step2, dy[dir] * step2);

            if (!cannonTarget.isValid()) {
              break;
            }

            ChessPiece targetPiece = board.getPieceAt(cannonTarget);
            if (targetPiece != null) {
              // 找到目标，检查是否可以吃
              if (targetPiece.getColor() != this.color) {
                moves.add(cannonTarget);
              }
              break;
            }
          }
          break;
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

    ChessPiece targetPiece = board.getPieceAt(target);

    if (targetPiece == null) {
      // 不吃子的移动，路径必须畅通
      return isPathClear(position, target, board);
    } else {
      // 吃子的移动，必须翻一个山
      return hasExactlyOnePieceInBetween(position, target, board);
    }
  }

  /**
   * 检查两个位置之间是否恰好有一个棋子作为炮台
   */
  private boolean hasExactlyOnePieceInBetween(Position from, Position to, ChessBoard board) {
    int rowDiff = to.getRow() - from.getRow();
    int colDiff = to.getCol() - from.getCol();

    int rowStep = Integer.compare(rowDiff, 0);
    int colStep = Integer.compare(colDiff, 0);

    int pieceCount = 0;
    int currentRow = from.getRow() + rowStep;
    int currentCol = from.getCol() + colStep;

    while (currentRow != to.getRow() || currentCol != to.getCol()) {
      if (board.getPieceAt(new Position(currentRow, currentCol)) != null) {
        pieceCount++;
      }
      currentRow += rowStep;
      currentCol += colStep;
    }

    return pieceCount == 1;
  }
}
