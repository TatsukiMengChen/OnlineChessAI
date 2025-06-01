package com.mimeng.chess.api.room;

/**
 * 房间数据模型
 */
public class Room {
  public String id;
  public String name;
  public Integer player1Id;
  public Integer player2Id;
  public String status; // waiting, playing, finished, full, closed
  public String createdAt;
  public String updatedAt;

  public Room() {
  }

  public Room(String id, String name, Integer player1Id, Integer player2Id,
      String status, String createdAt, String updatedAt) {
    this.id = id;
    this.name = name;
    this.player1Id = player1Id;
    this.player2Id = player2Id;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /**
   * 房间状态枚举
   */
  public static class Status {
    public static final String WAITING = "waiting";
    public static final String PLAYING = "playing";
    public static final String FINISHED = "finished";
    public static final String FULL = "full";
    public static final String CLOSED = "closed";
  }

  /**
   * 获取状态显示文本
   */
  public String getStatusText() {
    switch (status) {
      case Status.WAITING:
        return "等待中";
      case Status.PLAYING:
        return "游戏中";
      case Status.FINISHED:
        return "已结束";
      case Status.FULL:
        return "已满员";
      case Status.CLOSED:
        return "已关闭";
      default:
        return "未知";
    }
  }

  /**
   * 检查房间是否可以加入
   */
  public boolean canJoin() {
    return Status.WAITING.equals(status) && player2Id == null;
  }

  /**
   * 检查是否为房主
   */
  public boolean isOwner(int userId) {
    return player1Id != null && player1Id == userId;
  }

  /**
   * 检查是否为房间成员
   */
  public boolean isMember(int userId) {
    return (player1Id != null && player1Id == userId) ||
        (player2Id != null && player2Id == userId);
  }

  /**
   * 获取玩家数量
   */
  public int getPlayerCount() {
    int count = 0;
    if (player1Id != null)
      count++;
    if (player2Id != null)
      count++;
    return count;
  }

  /**
   * 获取玩家信息显示文本
   */
  public String getPlayerInfoText() {
    return getPlayerCount() + "/2";
  }
}
