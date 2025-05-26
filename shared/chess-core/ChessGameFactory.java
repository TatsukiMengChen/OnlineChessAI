package com.mimeng.chess.entity.chess;

/**
 * 象棋游戏工厂类 - 用于创建和管理游戏实例
 */
public class ChessGameFactory {

  /**
   * 创建人机对战游戏
   */
  public static ChessRoom createHumanVsAIGame(String roomName, Long humanUserId, String humanName,
      PlayerType aiDifficulty) {
    ChessRoom room = new ChessRoom(roomName);
    room.createNewGame();

    // 人类玩家作为红方
    room.joinPlayer(humanUserId, humanName, PlayerType.HUMAN);
    // AI作为黑方
    room.joinAI(aiDifficulty);

    return room;
  }

  /**
   * 创建人人对战游戏
   */
  public static ChessRoom createHumanVsHumanGame(String roomName) {
    ChessRoom room = new ChessRoom(roomName);
    room.createNewGame();
    return room;
  }

  /**
   * 创建私人房间
   */
  public static ChessRoom createPrivateRoom(String roomName, String password) {
    ChessRoom room = new ChessRoom(roomName);
    room.setPrivate(true);
    room.setPassword(password);
    room.createNewGame();
    return room;
  }

  /**
   * 创建观战房间
   */
  public static ChessRoom createSpectatorRoom(String roomName, int maxSpectators) {
    ChessRoom room = new ChessRoom(roomName);
    room.setAllowSpectators(true);
    room.setMaxSpectators(maxSpectators);
    room.createNewGame();
    return room;
  }

  /**
   * 创建锦标赛模式房间
   */
  public static ChessRoom createTournamentRoom(String roomName, int timeLimit) {
    ChessRoom room = new ChessRoom(roomName);
    room.setGameTimeLimit(timeLimit);
    room.setEnableUndo(false); // 锦标赛模式不允许悔棋
    room.setAllowSpectators(true);
    room.createNewGame();
    return room;
  }

  /**
   * 创建练习模式房间
   */
  public static ChessRoom createPracticeRoom(String playerName, Long userId) {
    ChessRoom room = new ChessRoom("练习模式 - " + playerName);
    room.setAllowSpectators(false);
    room.setEnableUndo(true);
    room.setMaxUndoCount(10); // 练习模式允许更多悔棋
    room.createNewGame();

    // 加入人类玩家和简单AI
    room.joinPlayer(userId, playerName, PlayerType.HUMAN);
    room.joinAI(PlayerType.AI_EASY);

    return room;
  }
}
