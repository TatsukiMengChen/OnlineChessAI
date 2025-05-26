package com.mimeng.chess.entity.chess;

/**
 * 象棋游戏使用示例
 * 展示如何使用设计的象棋类进行游戏开发
 */
public class ChessGameExample {

  /**
   * 基本游戏流程示例
   */
  public static void basicGameExample() {
    System.out.println("=== 基本游戏流程示例 ===");

    // 1. 创建房间和游戏
    ChessRoom room = ChessGameFactory.createHumanVsHumanGame("测试房间");

    // 2. 玩家加入
    room.joinPlayer(1001L, "张三", PlayerType.HUMAN);
    room.joinPlayer(1002L, "李四", PlayerType.HUMAN);

    // 3. 开始游戏
    if (room.startGame()) {
      System.out.println("游戏开始！");
      ChessGameState game = room.getGameState();

      // 4. 游戏循环示例
      demonstrateGameplay(game);
    }
  }

  /**
   * 演示游戏玩法
   */
  private static void demonstrateGameplay(ChessGameState game) {
    System.out.println("当前棋盘状态：");
    System.out.println(game.getBoard().toString());

    // 红方移动：兵三进一
    Position from = new Position(6, 2); // 红兵位置
    Position to = new Position(5, 2); // 向前一格

    if (game.tryMove(from, to)) {
      System.out.println("红方：兵三进一");
      System.out.println("移动成功！当前状态：" + game.getGameStatusSummary());
    }

    // 黑方移动：卒7进1
    from = new Position(3, 6); // 黑卒位置
    to = new Position(4, 6); // 向前一格

    if (game.tryMove(from, to)) {
      System.out.println("黑方：卒7进1");
      System.out.println("移动成功！当前状态：" + game.getGameStatusSummary());
    }

    System.out.println("移动历史：");
    for (String record : game.getGameRecord()) {
      System.out.println("- " + record);
    }
  }

  /**
   * 人机对战示例
   */
  public static void humanVsAIExample() {
    System.out.println("\n=== 人机对战示例 ===");

    // 创建人机对战房间
    ChessRoom room = ChessGameFactory.createHumanVsAIGame(
        "人机对战", 1001L, "玩家1", PlayerType.AI_MEDIUM);

    if (room.startGame()) {
      System.out.println("人机对战开始！");
      ChessGameState game = room.getGameState();

      System.out.println("红方：" + game.getRedPlayer().getName() + " (" + game.getRedPlayer().getType() + ")");
      System.out.println("黑方：" + game.getBlackPlayer().getName() + " (" + game.getBlackPlayer().getType() + ")");
    }
  }

  /**
   * 前端交互示例（模拟Android客户端）
   */
  public static void frontendInteractionExample() {
    System.out.println("\n=== 前端交互示例 ===");

    ChessRoom room = ChessGameFactory.createHumanVsHumanGame("前端测试");
    room.joinPlayer(1001L, "前端玩家", PlayerType.HUMAN);
    room.joinPlayer(1002L, "后端玩家", PlayerType.HUMAN);

    if (room.startGame()) {
      ChessGameState game = room.getGameState();

      // 模拟前端选择棋子
      Position piecePosition = new Position(6, 0); // 选择红方的兵
      if (game.selectPiece(piecePosition)) {
        System.out.println("选中棋子：" + piecePosition);
        System.out.println("可移动位置：");
        for (Position move : game.getAvailableMoves()) {
          System.out.println("  - " + move);
        }

        // 模拟移动到第一个可用位置
        if (!game.getAvailableMoves().isEmpty()) {
          Position targetPosition = game.getAvailableMoves().get(0);
          if (game.selectPiece(targetPosition)) {
            System.out.println("移动到：" + targetPosition);
          }
        }
      }
    }
  }

  /**
   * Socket消息处理示例（模拟后端处理）
   */
  public static void socketMessageExample() {
    System.out.println("\n=== Socket消息处理示例 ===");

    ChessRoom room = ChessGameFactory.createHumanVsHumanGame("在线对战");

    // 模拟Socket消息：玩家加入
    handlePlayerJoin(room, 1001L, "在线玩家1");
    handlePlayerJoin(room, 1002L, "在线玩家2");

    // 模拟Socket消息：开始游戏
    handleGameStart(room);

    // 模拟Socket消息：玩家移动
    handlePlayerMove(room, 1001L, new Position(6, 0), new Position(5, 0));
  }

  /**
   * 处理玩家加入消息
   */
  private static void handlePlayerJoin(ChessRoom room, Long userId, String playerName) {
    if (room.joinPlayer(userId, playerName, PlayerType.HUMAN)) {
      System.out.println("玩家 " + playerName + " 加入房间");
      broadcastRoomUpdate(room);
    }
  }

  /**
   * 处理游戏开始消息
   */
  private static void handleGameStart(ChessRoom room) {
    if (room.startGame()) {
      System.out.println("游戏开始广播发送");
      broadcastGameStart(room);
    }
  }

  /**
   * 处理玩家移动消息
   */
  private static void handlePlayerMove(ChessRoom room, Long userId, Position from, Position to) {
    if (room.playerMove(userId, from, to)) {
      System.out.println("玩家 " + userId + " 移动棋子 " + from + " -> " + to);
      broadcastMove(room, from, to);

      // 检查游戏是否结束
      ChessGameState game = room.getGameState();
      if (game.getStatus() != GameStatus.PLAYING) {
        broadcastGameEnd(room, game.getStatus());
      }
    }
  }

  /**
   * 广播房间更新
   */
  private static void broadcastRoomUpdate(ChessRoom room) {
    System.out.println("广播：房间更新 - " + room.getRoomSummary());
  }

  /**
   * 广播游戏开始
   */
  private static void broadcastGameStart(ChessRoom room) {
    System.out.println("广播：游戏开始");
  }

  /**
   * 广播移动
   */
  private static void broadcastMove(ChessRoom room, Position from, Position to) {
    System.out.println("广播：棋子移动 " + from + " -> " + to);
  }

  /**
   * 广播游戏结束
   */
  private static void broadcastGameEnd(ChessRoom room, GameStatus result) {
    System.out.println("广播：游戏结束 - " + result);
  }

  /**
   * 主函数 - 运行所有示例
   */
  public static void main(String[] args) {
    basicGameExample();
    humanVsAIExample();
    frontendInteractionExample();
    socketMessageExample();

    System.out.println("\n=== 示例运行完成 ===");
  }
}
