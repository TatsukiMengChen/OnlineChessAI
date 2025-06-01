package com.mimeng.chess.entity.chess;

import java.util.Objects;

/**
 * 象棋移动操作类
 */
public class Move {
  private final Position from; // 起始位置
  private final Position to; // 目标位置
  private final ChessPiece piece; // 移动的棋子
  private final ChessPiece capturedPiece; // 被吃掉的棋子（可能为null）

  public Move(Position from, Position to, ChessPiece piece, ChessPiece capturedPiece) {
    this.from = from;
    this.to = to;
    this.piece = piece;
    this.capturedPiece = capturedPiece;
  }

  public Position getFrom() {
    return from;
  }

  public Position getTo() {
    return to;
  }

  public ChessPiece getPiece() {
    return piece;
  }

  public ChessPiece getCapturedPiece() {
    return capturedPiece;
  }

  public boolean isCapture() {
    return capturedPiece != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Move move = (Move) o;
    return Objects.equals(from, move.from) &&
        Objects.equals(to, move.to) &&
        Objects.equals(piece, move.piece);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, piece);
  }

  @Override
  public String toString() {
    return piece.getType().getName(piece.getColor()) +
        " from " + from + " to " + to +
        (isCapture() ? " (captures " + capturedPiece.getType().getName(capturedPiece.getColor()) + ")" : "");
  }
}
