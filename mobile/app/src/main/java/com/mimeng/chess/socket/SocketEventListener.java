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
   * 玩家加入房间
   */
  void onPlayerJoined(JsonObject data);

  /**
   * 玩家离开房间
   */
  void onPlayerLeft(JsonObject data);

  /**
   * 玩家准备状态变化
   */
  void onPlayerReadyChanged(JsonObject data);

  /**
   * 房间状态更新
   */
  void onRoomStatus(JsonObject data);

  /**
   * 游戏状态更新
   */
  void onGameState(JsonObject data);

  /**
   * 游戏开始
   */
  void onGameStarted(JsonObject gameState);

  /**
   * 玩家投降
   */
  void onPlayerSurrendered(JsonObject data);

  /**
   * 房间即将关闭
   */
  void onRoomClosing(JsonObject data);

  /**
   * 房间被关闭
   */
  void onRoomClosed(String message);

  /**
   * 房间不存在
   */
  void onRoomNotFound(String message);

  /**
   * 连接错误
   */
  void onError(String error);
}
