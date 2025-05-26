package com.mimeng.chess.entity.chess;

import com.mimeng.chess.entity.Room;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 象棋房间类 - 扩展基础Room类，添加象棋游戏功能
 * 可用于后端Socket消息处理和前端状态管理
 */
public class ChessRoom extends Room {

  // 游戏相关
  private ChessGameState gameState; // 当前游戏状态
  private List<String> spectators; // 观战者列表
  private boolean isPrivate; // 是否私人房间
  private String password; // 房间密码（如果是私人房间）

  // 房间设置
  private int maxSpectators; // 最大观战人数
  private boolean allowSpectators; // 是否允许观战
  private int gameTimeLimit; // 游戏时间限制（分钟）
  private boolean enableUndo; // 是否允许悔棋
  private int maxUndoCount; // 最大悔棋次数

  // 房间状态
  private LocalDateTime lastActivity; // 最后活动时间
  private int totalGames; // 总游戏局数
  private int redWins; // 红方胜局
  private int blackWins; // 黑方胜局
  private int draws; // 平局数

  public ChessRoom() {
    super();
    this.spectators = new ArrayList<>();
    this.isPrivate = false;
    this.maxSpectators = 10;
    this.allowSpectators = true;
    this.gameTimeLimit = 30; // 30分钟
    this.enableUndo = true;
    this.maxUndoCount = 3;
    this.lastActivity = LocalDateTime.now();
    this.totalGames = 0;
    this.redWins = 0;
    this.blackWins = 0;
    this.draws = 0;
  }

  public ChessRoom(String roomName) {
    this();
    this.setName(roomName);
  }

  /**
   * 创建新游戏
   */
  public boolean createNewGame() {
    if (getPlayer1Id() == null || getPlayer2Id() == null) {
      return false;
    }

    String gameId = generateGameId();
    this.gameState = new ChessGameState(gameId, this.getId());

    // 设置游戏参数
    gameState.setUseTimer(true);
    gameState.setAllowUndo(enableUndo);
    gameState.setMaxUndoCount(maxUndoCount);
    gameState.setRedTimeLeft(gameTimeLimit * 60);
    gameState.setBlackTimeLeft(gameTimeLimit * 60);

    updateLastActivity();
    return true;
  }

  /**
   * 加入玩家
   */
  public boolean joinPlayer(Long userId, String playerName, PlayerType playerType) {
    if (getPlayer1Id() == null) {
      setPlayer1Id(userId);
      if (gameState != null) {
        Player redPlayer = new Player(userId, playerName, playerType, PlayerColor.RED);
        gameState.setRedPlayer(redPlayer);
      }
      updateLastActivity();
      return true;
    } else if (getPlayer2Id() == null && !getPlayer1Id().equals(userId)) {
      setPlayer2Id(userId);
      if (gameState != null) {
        Player blackPlayer = new Player(userId, playerName, playerType, PlayerColor.BLACK);
        gameState.setBlackPlayer(blackPlayer);
      }
      updateLastActivity();
      return true;
    }
    return false;
  }

  /**
   * 加入AI玩家
   */
  public boolean joinAI(PlayerType aiType) {
    if (getPlayer1Id() == null) {
      setPlayer1Id(-1L); // AI使用负数ID
      if (gameState != null) {
        Player aiPlayer = Player.createAI(aiType, PlayerColor.RED);
        gameState.setRedPlayer(aiPlayer);
      }
      updateLastActivity();
      return true;
    } else if (getPlayer2Id() == null) {
      setPlayer2Id(-2L); // AI使用负数ID
      if (gameState != null) {
        Player aiPlayer = Player.createAI(aiType, PlayerColor.BLACK);
        gameState.setBlackPlayer(aiPlayer);
      }
      updateLastActivity();
      return true;
    }
    return false;
  }

  /**
   * 玩家离开
   */
  public boolean leavePlayer(Long userId) {
    if (getPlayer1Id() != null && getPlayer1Id().equals(userId)) {
      setPlayer1Id(null);
      if (gameState != null) {
        gameState.setRedPlayer(null);
        if (gameState.getStatus() == GameStatus.PLAYING) {
          gameState.setStatus(GameStatus.ABANDONED);
        }
      }
      updateLastActivity();
      return true;
    } else if (getPlayer2Id() != null && getPlayer2Id().equals(userId)) {
      setPlayer2Id(null);
      if (gameState != null) {
        gameState.setBlackPlayer(null);
        if (gameState.getStatus() == GameStatus.PLAYING) {
          gameState.setStatus(GameStatus.ABANDONED);
        }
      }
      updateLastActivity();
      return true;
    }
    return false;
  }

  /**
   * 加入观战者
   */
  public boolean joinSpectator(String spectatorId) {
    if (!allowSpectators || spectators.size() >= maxSpectators) {
      return false;
    }
    if (!spectators.contains(spectatorId)) {
      spectators.add(spectatorId);
      updateLastActivity();
      return true;
    }
    return false;
  }

  /**
   * 移除观战者
   */
  public boolean removeSpectator(String spectatorId) {
    boolean removed = spectators.remove(spectatorId);
    if (removed) {
      updateLastActivity();
    }
    return removed;
  }

  /**
   * 开始游戏
   */
  public boolean startGame() {
    if (gameState == null) {
      createNewGame();
    }

    if (gameState != null && gameState.startGame()) {
      setStatus("playing");
      updateLastActivity();
      return true;
    }
    return false;
  }

  /**
   * 结束游戏
   */
  public void endGame(GameStatus result) {
    if (gameState != null) {
      gameState.setStatus(result);
      updateGameStatistics(result);
      setStatus("finished");
      updateLastActivity();
    }
  }

  /**
   * 更新游戏统计
   */
  private void updateGameStatistics(GameStatus result) {
    totalGames++;
    switch (result) {
      case RED_WIN:
        redWins++;
        break;
      case BLACK_WIN:
        blackWins++;
        break;
      case DRAW:
        draws++;
        break;
    }
  }

  /**
   * 玩家移动
   */
  public boolean playerMove(Long userId, Position from, Position to) {
    if (gameState == null || gameState.getStatus() != GameStatus.PLAYING) {
      return false;
    }

    if (!gameState.isPlayerTurn(userId)) {
      return false;
    }

    boolean success = gameState.tryMove(from, to);
    if (success) {
      updateLastActivity();
    }
    return success;
  }

  /**
   * 玩家选择棋子
   */
  public boolean playerSelectPiece(Long userId, Position position) {
    if (gameState == null || gameState.getStatus() != GameStatus.PLAYING) {
      return false;
    }

    if (!gameState.isPlayerTurn(userId)) {
      return false;
    }

    boolean success = gameState.selectPiece(position);
    if (success) {
      updateLastActivity();
    }
    return success;
  }

  /**
   * 检查房间是否可用
   */
  public boolean isAvailable() {
    return getPlayer1Id() == null || getPlayer2Id() == null;
  }

  /**
   * 检查房间是否已满
   */
  public boolean isFull() {
    return getPlayer1Id() != null && getPlayer2Id() != null;
  }

  /**
   * 检查用户是否在房间中
   */
  public boolean hasUser(Long userId) {
    return (getPlayer1Id() != null && getPlayer1Id().equals(userId)) ||
        (getPlayer2Id() != null && getPlayer2Id().equals(userId));
  }

  /**
   * 获取房间摘要信息
   */
  public String getRoomSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append("房间: ").append(getName()).append("\n");
    summary.append("状态: ").append(getStatus()).append("\n");
    summary.append("玩家: ").append(getPlayer1Id() != null ? "红方已就位" : "等待红方");
    summary.append(" | ").append(getPlayer2Id() != null ? "黑方已就位" : "等待黑方").append("\n");
    summary.append("观战: ").append(spectators.size()).append("/").append(maxSpectators).append("\n");
    if (gameState != null) {
      summary.append("游戏: ").append(gameState.getGameStatusSummary());
    }
    return summary.toString();
  }

  /**
   * 更新最后活动时间
   */
  private void updateLastActivity() {
    this.lastActivity = LocalDateTime.now();
    this.setUpdatedAt(LocalDateTime.now());
  }

  /**
   * 生成游戏ID
   */
  private String generateGameId() {
    return "game_" + System.currentTimeMillis() + "_" + Math.random();
  }

  // Getters and Setters
  public ChessGameState getGameState() {
    return gameState;
  }

  public void setGameState(ChessGameState gameState) {
    this.gameState = gameState;
  }

  public List<String> getSpectators() {
    return new ArrayList<>(spectators);
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getMaxSpectators() {
    return maxSpectators;
  }

  public void setMaxSpectators(int maxSpectators) {
    this.maxSpectators = maxSpectators;
  }

  public boolean isAllowSpectators() {
    return allowSpectators;
  }

  public void setAllowSpectators(boolean allowSpectators) {
    this.allowSpectators = allowSpectators;
  }

  public int getGameTimeLimit() {
    return gameTimeLimit;
  }

  public void setGameTimeLimit(int gameTimeLimit) {
    this.gameTimeLimit = gameTimeLimit;
  }

  public boolean isEnableUndo() {
    return enableUndo;
  }

  public void setEnableUndo(boolean enableUndo) {
    this.enableUndo = enableUndo;
  }

  public int getMaxUndoCount() {
    return maxUndoCount;
  }

  public void setMaxUndoCount(int maxUndoCount) {
    this.maxUndoCount = maxUndoCount;
  }

  public LocalDateTime getLastActivity() {
    return lastActivity;
  }

  public int getTotalGames() {
    return totalGames;
  }

  public int getRedWins() {
    return redWins;
  }

  public int getBlackWins() {
    return blackWins;
  }

  public int getDraws() {
    return draws;
  }
}
