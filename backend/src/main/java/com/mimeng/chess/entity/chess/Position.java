package com.mimeng.chess.entity.chess;

import java.util.Objects;

/**
 * 棋盘坐标类
 * 中国象棋棋盘：10行9列 (0-9, 0-8)
 */
public class Position {
  private final int row; // 行 (0-9)
  private final int col; // 列 (0-8)

  public Position(int row, int col) {
    this.row = row;
    this.col = col;
  }

  public int getRow() {
    return row;
  }

  public int getCol() {
    return col;
  }

  /**
   * 检查坐标是否在棋盘范围内
   */
  public boolean isValid() {
    return row >= 0 && row <= 9 && col >= 0 && col <= 8;
  }

  /**
   * 检查是否在红方九宫格内
   */
  public boolean isInRedPalace() {
    return row >= 7 && row <= 9 && col >= 3 && col <= 5;
  }

  /**
   * 检查是否在黑方九宫格内
   */
  public boolean isInBlackPalace() {
    return row >= 0 && row <= 2 && col >= 3 && col <= 5;
  }

  /**
   * 检查是否在红方半场
   */
  public boolean isInRedSide() {
    return row >= 5;
  }

  /**
   * 检查是否在黑方半场
   */
  public boolean isInBlackSide() {
    return row <= 4;
  }

  /**
   * 检查是否在河界上
   */
  public boolean isOnRiver() {
    return row == 4 || row == 5;
  }

  /**
   * 计算到另一个位置的距离
   */
  public int distanceTo(Position other) {
    return Math.abs(this.row - other.row) + Math.abs(this.col - other.col);
  }

  /**
   * 创建新的位置
   */
  public Position move(int deltaRow, int deltaCol) {
    return new Position(row + deltaRow, col + deltaCol);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Position position = (Position) o;
    return row == position.row && col == position.col;
  }

  @Override
  public int hashCode() {
    return Objects.hash(row, col);
  }

  @Override
  public String toString() {
    return "(" + row + "," + col + ")";
  }
}
