package com.mimeng.chess.entity.chess;

/**
 * 玩家信息类
 */
public class Player {
  private Long userId; // 用户ID（如果是人类玩家）
  private String name; // 玩家名称
  private PlayerType type; // 玩家类型
  private PlayerColor color; // 棋子颜色
  private boolean isOnline; // 是否在线
  private int timeLeft; // 剩余时间（秒）
  private boolean isReady; // 是否准备就绪

  public Player() {
  }

  public Player(Long userId, String name, PlayerType type, PlayerColor color) {
    this.userId = userId;
    this.name = name;
    this.type = type;
    this.color = color;
    this.isOnline = true;
    this.timeLeft = 1800; // 默认30分钟
    this.isReady = false;
  }

  /**
   * 创建AI玩家
   */
  public static Player createAI(PlayerType aiType, PlayerColor color) {
    String aiName = "AI-" + aiType.name();
    Player ai = new Player(null, aiName, aiType, color);
    ai.isReady = true;
    ai.isOnline = true;
    return ai;
  }

  /**
   * 检查是否是AI玩家
   */
  public boolean isAI() {
    return type != PlayerType.HUMAN;
  }

  /**
   * 检查是否是人类玩家
   */
  public boolean isHuman() {
    return type == PlayerType.HUMAN;
  }

  // Getters and Setters
  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PlayerType getType() {
    return type;
  }

  public void setType(PlayerType type) {
    this.type = type;
  }

  public PlayerColor getColor() {
    return color;
  }

  public void setColor(PlayerColor color) {
    this.color = color;
  }

  public boolean isOnline() {
    return isOnline;
  }

  public void setOnline(boolean online) {
    isOnline = online;
  }

  public int getTimeLeft() {
    return timeLeft;
  }

  public void setTimeLeft(int timeLeft) {
    this.timeLeft = timeLeft;
  }

  public boolean isReady() {
    return isReady;
  }

  public void setReady(boolean ready) {
    isReady = ready;
  }

  @Override
  public String toString() {
    return name + "(" + color + ", " + type + ")";
  }
}
