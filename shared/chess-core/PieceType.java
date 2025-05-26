package com.mimeng.chess.entity.chess;

/**
 * 中国象棋棋子类型枚举
 */
public enum PieceType {
  KING("将", "帅"), // 将/帅
  GUARD("仕", "仕"), // 仕/仕
  ELEPHANT("象", "相"), // 象/相
  HORSE("马", "马"), // 马
  ROOK("车", "车"), // 车
  CANNON("炮", "炮"), // 炮
  PAWN("卒", "兵"); // 卒/兵

  private final String redName;
  private final String blackName;

  PieceType(String redName, String blackName) {
    this.redName = redName;
    this.blackName = blackName;
  }

  public String getRedName() {
    return redName;
  }

  public String getBlackName() {
    return blackName;
  }

  public String getName(PlayerColor color) {
    return color == PlayerColor.RED ? redName : blackName;
  }
}
