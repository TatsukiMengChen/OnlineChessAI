package com.mimeng.chess.socket;

import com.mimeng.chess.entity.chess.*;
import com.mimeng.chess.service.RoomService;
import com.mimeng.chess.entity.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 游戏状态管理器
 * 负责管理所有活跃房间的游戏状态
 */
@Component
public class GameManager {

  @Autowired
  private RoomService roomService;

  // 存储活跃的游戏房间 roomId -> ChessRoom
  private final Map<String, ChessRoom> activeRooms = new ConcurrentHashMap<>();

  /**
   * 获取或创建游戏房间
   */
  public ChessRoom getOrCreateGameRoom(String roomId) {
    return activeRooms.computeIfAbsent(roomId, id -> {
      // 从数据库获取房间信息
      Room room = roomService.getById(id);
      if (room == null) {
        return null;
      }

      // 创建象棋房间
      ChessRoom chessRoom = new ChessRoom(room.getName());
      chessRoom.setId(room.getId());
      chessRoom.setStatus(room.getStatus());
      chessRoom.setPlayer1Id(room.getPlayer1Id());
      chessRoom.setPlayer2Id(room.getPlayer2Id());

      return chessRoom;
    });
  }

  /**
   * 移除房间
   */
  public void removeRoom(String roomId) {
    activeRooms.remove(roomId);
  }

  /**
   * 玩家加入房间
   */
  public boolean joinRoom(String roomId, Long userId, String playerName) {
    ChessRoom room = getOrCreateGameRoom(roomId);
    if (room == null) {
      return false;
    }

    // 确保创建游戏状态
    if (room.getGameState() == null) {
      room.createNewGame();
    }

    // 加入玩家
    return room.joinPlayer(userId, playerName, PlayerType.HUMAN);
  }

  /**
   * 玩家离开房间
   */
  public boolean leaveRoom(String roomId, Long userId) {
    ChessRoom room = activeRooms.get(roomId);
    if (room == null) {
      return false;
    }

    boolean success = room.leavePlayer(userId);

    // 如果房间空了，移除房间
    if (room.getPlayer1Id() == null && room.getPlayer2Id() == null) {
      removeRoom(roomId);
    }

    return success;
  }

  /**
   * 玩家准备
   */
  public boolean playerReady(String roomId, Long userId, boolean ready) {
    ChessRoom room = activeRooms.get(roomId);
    if (room == null || room.getGameState() == null) {
      return false;
    }

    ChessGameState gameState = room.getGameState();
    Player player = null;

    if (room.getPlayer1Id() != null && room.getPlayer1Id().equals(userId)) {
      player = gameState.getRedPlayer();
    } else if (room.getPlayer2Id() != null && room.getPlayer2Id().equals(userId)) {
      player = gameState.getBlackPlayer();
    }

    if (player != null) {
      player.setReady(ready);
      return true;
    }

    return false;
  }

  /**
   * 尝试开始游戏
   */
  public boolean tryStartGame(String roomId) {
    ChessRoom room = activeRooms.get(roomId);
    if (room == null) {
      return false;
    }

    return room.startGame();
  }

  /**
   * 玩家移动
   */
  public boolean playerMove(String roomId, Long userId, Position from, Position to) {
    ChessRoom room = activeRooms.get(roomId);
    if (room == null) {
      return false;
    }

    return room.playerMove(userId, from, to);
  }

  /**
   * 玩家选择棋子
   */
  public boolean playerSelectPiece(String roomId, Long userId, Position position) {
    ChessRoom room = activeRooms.get(roomId);
    if (room == null) {
      return false;
    }

    return room.playerSelectPiece(userId, position);
  }

  /**
   * 获取游戏状态
   */
  public ChessGameState getGameState(String roomId) {
    ChessRoom room = activeRooms.get(roomId);
    return room != null ? room.getGameState() : null;
  }

  /**
   * 投降
   */
  public boolean surrender(String roomId, Long userId) {
    ChessRoom room = activeRooms.get(roomId);
    if (room == null || room.getGameState() == null) {
      return false;
    }

    ChessGameState gameState = room.getGameState();
    if (gameState.isPlayerTurn(userId)) {
      gameState.surrender();
      return true;
    }

    return false;
  }

  /**
   * 悔棋
   */
  public boolean undoMove(String roomId, Long userId) {
    ChessRoom room = activeRooms.get(roomId);
    if (room == null || room.getGameState() == null) {
      return false;
    }

    ChessGameState gameState = room.getGameState();
    // 简单实现：只有轮到该玩家时才能悔棋
    if (gameState.isPlayerTurn(userId)) {
      return gameState.undoMove();
    }

    return false;
  }
}
