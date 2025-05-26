package com.mimeng.chess.entity.chess;

import java.util.List;

/**
 * 象棋棋子抽象基类
 */
public abstract class ChessPiece {
  protected final PieceType type;
  protected final PlayerColor color;
  protected Position position;

  public ChessPiece(PieceType type, PlayerColor color, Position position) {
    this.type = type;
    this.color = color;
    this.position = position;
  }

  public PieceType getType() {
    return type;
  }

  public PlayerColor getColor() {
    return color;
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  /**
   * 获取所有可能的移动位置
   * 
   * @param board 当前棋盘状态
   * @return 可移动的位置列表
   */
  public abstract List<Position> getPossibleMoves(ChessBoard board);

  /**
   * 检查是否可以移动到指定位置
   * 
   * @param target 目标位置
   * @param board  当前棋盘状态
   * @return 是否可以移动
   */
  public abstract boolean canMoveTo(Position target, ChessBoard board);

  /**
   * 获取棋子的显示名称
   */
  public String getName() {
    return type.getName(color);
  }

  /**
   * 检查路径是否被阻挡（用于车、炮等需要直线移动的棋子）
   */
  protected boolean isPathClear(Position from, Position to, ChessBoard board) {
    int rowDiff = to.getRow() - from.getRow();
    int colDiff = to.getCol() - from.getCol();

    // 不是直线移动
    if (rowDiff != 0 && colDiff != 0) {
      return false;
    }

    int rowStep = Integer.compare(rowDiff, 0);
    int colStep = Integer.compare(colDiff, 0);

    int currentRow = from.getRow() + rowStep;
    int currentCol = from.getCol() + colStep;

    while (currentRow != to.getRow() || currentCol != to.getCol()) {
      if (board.getPieceAt(new Position(currentRow, currentCol)) != null) {
        return false;
      }
      currentRow += rowStep;
      currentCol += colStep;
    }

    return true;
  }

  /**
   * 检查目标位置是否可以放置（空位或敌方棋子）
   */
  protected boolean isValidTarget(Position target, ChessBoard board) {
    if (!target.isValid()) {
      return false;
    }

    ChessPiece targetPiece = board.getPieceAt(target);
    return targetPiece == null || targetPiece.getColor() != this.color;
  }

  @Override
  public String toString() {
    return getName() + "@" + position;
  }
}
