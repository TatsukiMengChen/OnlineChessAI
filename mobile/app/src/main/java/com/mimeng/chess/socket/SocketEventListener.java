package com.mimeng.chess.socket;

import com.google.gson.JsonObject;

/**
 * Socket.IO 事件监听接口
 */
public interface SocketEventListener {

  /**
   * 连接状态变化
   */
  void onConnectionChanged(boolean connected);

  /**
   * 认证成功
   */
  void onAuthSuccess();

  /**
   * 认证失败
   */
  void onAuthFail(String message);

  /**
   * 房间状态更新
   */
  void onRoomStateUpdated(JsonObject roomState);

  /**
   * 用户进入房间
   */
  void onUserEntered(String username);

  /**
   * 用户离开房间
   */
  void onUserLeft(String username);

  /**
   * 玩家准备状态变化
   */
  void onPlayerReadyChanged(String username, boolean ready);

  /**
   * 游戏开始
   */
  void onGameStarted(JsonObject gameState);

  /**
   * 连接错误
   */
  void onError(String error);
}
