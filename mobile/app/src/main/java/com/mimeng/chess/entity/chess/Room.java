package com.mimeng.chess.entity.chess;

/**
 * 房间数据模型
 */
public class Room {
  private String id; // Changed to private, use getters/setters
  private String name; // Changed to private, use getters/setters
  private Long player1Id; // Changed to private, use getters/setters
  private Long player2Id; // Changed to private, use getters/setters
  private String status; // Changed to private, use getters/setters
  private String createdAt; // Changed to private, use getters/setters
  private String updatedAt; // Changed to private, use getters/setters

  public Room() {
    // Initialize with current time as String if needed, or leave null
    // this.createdAt = LocalDateTime.now().format(formatter);
    // this.updatedAt = LocalDateTime.now().format(formatter);
    // Simple ID generation, ensure it's unique or handled by backend
    this.id = "room_" + System.currentTimeMillis();
    this.status = Status.WAITING; // Default status
  }

  public Room(String id, String name, Long player1Id, Long player2Id,
      String status, String createdAt, String updatedAt) {
    this.id = id;
    this.name = name;
    this.player1Id = player1Id;
    this.player2Id = player2Id;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  // Getters and Setters to be used by ChessRoom
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getPlayer1Id() {
    return player1Id;
  }

  public void setPlayer1Id(Long player1Id) {
    this.player1Id = player1Id;
  }

  public Long getPlayer2Id() {
    return player2Id;
  }

  public void setPlayer2Id(Long player2Id) {
    this.player2Id = player2Id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
    // If you had a lastActivity field tied to updatedAt in the original Room
    // ensure it's handled here or in ChessRoom appropriately.
    // For now, ChessRoom directly uses its own lastActivity.
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
    return player1Id != null && player1Id.equals(Long.valueOf(userId));
  }

  /**
   * 检查是否为房间成员
   */
  public boolean isMember(int userId) {
    Long userIdLong = Long.valueOf(userId);
    return (player1Id != null && player1Id.equals(userIdLong)) ||
        (player2Id != null && player2Id.equals(userIdLong));
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
