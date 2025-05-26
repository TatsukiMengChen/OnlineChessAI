package com.mimeng.chess.entity.chess.pieces;

import com.mimeng.chess.entity.chess.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 兵/卒
 */
public class Pawn extends ChessPiece {

  public Pawn(PlayerColor color, Position position) {
    super(PieceType.PAWN, color, position);
  }

  @Override
  public List<Position> getPossibleMoves(ChessBoard board) {
    List<Position> moves = new ArrayList<>();

    if (color == PlayerColor.RED) {
      // 红兵：向上移动（行数减少）
      Position forward = position.move(-1, 0);
      if (canMoveTo(forward, board)) {
        moves.add(forward);
      }

      // 过河后可以左右移动
      if (position.getRow() <= 4) {
        Position left = position.move(0, -1);
        Position right = position.move(0, 1);
        if (canMoveTo(left, board)) {
          moves.add(left);
        }
        if (canMoveTo(right, board)) {
          moves.add(right);
        }
      }
    } else {
      // 黑卒：向下移动（行数增加）
      Position forward = position.move(1, 0);
      if (canMoveTo(forward, board)) {
        moves.add(forward);
      }

      // 过河后可以左右移动
      if (position.getRow() >= 5) {
        Position left = position.move(0, -1);
        Position right = position.move(0, 1);
        if (canMoveTo(left, board)) {
          moves.add(left);
        }
        if (canMoveTo(right, board)) {
          moves.add(right);
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

    int rowDiff = target.getRow() - position.getRow();
    int colDiff = target.getCol() - position.getCol();

    // 只能移动一格
    if (Math.abs(rowDiff) + Math.abs(colDiff) != 1) {
      return false;
    }

    if (color == PlayerColor.RED) {
      // 红兵只能向前或（过河后）左右移动
      if (rowDiff > 0) {
        return false; // 不能后退
      }
      if (colDiff != 0 && position.getRow() > 4) {
        return false; // 未过河不能左右移动
      }
    } else {
      // 黑卒只能向前或（过河后）左右移动
      if (rowDiff < 0) {
        return false; // 不能后退
      }
      if (colDiff != 0 && position.getRow() < 5) {
        return false; // 未过河不能左右移动
      }
    }

    return true;
  }
}
