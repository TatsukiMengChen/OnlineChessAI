package com.mimeng.chess.entity.chess;

/**
 * 游戏状态枚举
 */
public enum GameStatus {
  WAITING, // 等待玩家加入
  PLAYING, // 游戏进行中
  RED_WIN, // 红方胜利
  BLACK_WIN, // 黑方胜利
  DRAW, // 平局
  PAUSED, // 暂停
  ABANDONED // 放弃/离开
}
