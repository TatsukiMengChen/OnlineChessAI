package com.mimeng.chess.socket;

import com.mimeng.chess.entity.chess.*;
import com.mimeng.chess.service.RoomService;
import com.mimeng.chess.entity.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 游戏状态管理器
 * 负责管理所有活跃房间的游戏状态
 */
@Component
public class GameManager {
  private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

  @Autowired
  private RoomService roomService;

  @Autowired
  private StringRedisTemplate redisTemplate;

  // 存储活跃的游戏房间 roomId -> ChessRoom
  private final Map<String, ChessRoom> activeRooms = new ConcurrentHashMap<>();

  /**
   * 获取或创建游戏房间
   */
  public ChessRoom getOrCreateGameRoom(String roomId) {
    return activeRooms.computeIfAbsent(roomId, id -> {
      try {
        logger.info("Attempting to create game room for roomId: {}", roomId);

        // 首先检查Redis中是否有该房间的等待信息
        String redisKey = "room:waiting:" + roomId;
        String redisValue = redisTemplate.opsForValue().get(redisKey);

        if (redisValue != null) {
          logger.info("Found room {} in Redis waiting list", roomId);
          // 从数据库获取房间信息
          Room room = roomService.getById(roomId);
          if (room != null) {
            logger.info("Found room {} in database: name={}, status={}, player1={}, player2={}",
                roomId, room.getName(), room.getStatus(), room.getPlayer1Id(), room.getPlayer2Id());

            // 创建象棋房间
            ChessRoom chessRoom = new ChessRoom(room.getName());
            chessRoom.setId(room.getId());
            chessRoom.setStatus(room.getStatus());
            chessRoom.setPlayer1Id(room.getPlayer1Id());
            chessRoom.setPlayer2Id(room.getPlayer2Id());

            logger.info("Successfully created ChessRoom for {}", roomId);
            return chessRoom;
          } else {
            logger.warn("Room {} exists in Redis but not in database", roomId);
          }
        } else {
          logger.warn("Room {} not found in Redis waiting list", roomId);

          // 尝试直接从数据库查找（可能是已经开始的游戏）
          Room room = roomService.getById(roomId);
          if (room != null) {
            logger.info("Found existing room {} in database: status={}", roomId, room.getStatus());

            ChessRoom chessRoom = new ChessRoom(room.getName());
            chessRoom.setId(room.getId());
            chessRoom.setStatus(room.getStatus());
            chessRoom.setPlayer1Id(room.getPlayer1Id());
            chessRoom.setPlayer2Id(room.getPlayer2Id());

            return chessRoom;
          }
        }

        logger.error("Room {} not found in Redis or database", roomId);
        return null;
      } catch (Exception e) {
        logger.error("Error creating game room for {}: {}", roomId, e.getMessage(), e);
        return null;
      }
    });
  }

  /**
   * 移除房间
   */
  public void removeRoom(String roomId) {
    activeRooms.remove(roomId);
    logger.info("Removed room {} from active rooms", roomId);
  }

  /**
   * 玩家加入房间
   */
  public boolean joinRoom(String roomId, Long userId, String playerName) {
    logger.info("User {} attempting to join room {}", userId, roomId);

    ChessRoom room = getOrCreateGameRoom(roomId);
    if (room == null) {
      logger.error("Failed to get or create game room {}", roomId);
      return false;
    } // 检查用户是否有权限加入该房间
    boolean canJoin = false;
    if (room.getPlayer1Id() != null && room.getPlayer1Id().equals(userId)) {
      canJoin = true;
      logger.info("User {} is player1 of room {}", userId, roomId);
    } else if (room.getPlayer2Id() != null && room.getPlayer2Id().equals(userId)) {
      canJoin = true;
      logger.info("User {} is player2 of room {}", userId, roomId);
    } else if (room.getPlayer2Id() == null) {
      // 如果player2为null，允许新玩家加入作为player2
      canJoin = true;
      logger.info("User {} joining as player2 in room {} (player2 slot is empty)", userId, roomId);

      // 更新数据库中的player2Id
      try {
        Room dbRoom = roomService.getById(roomId);
        if (dbRoom != null) {
          dbRoom.setPlayer2Id(userId);
          roomService.updateById(dbRoom);
          // 同时更新内存中的房间信息
          room.setPlayer2Id(userId);
          logger.info("Updated player2Id to {} for room {} in database and memory", userId, roomId);
        }
      } catch (Exception e) {
        logger.error("Failed to update player2Id for room {}: {}", roomId, e.getMessage(), e);
        return false;
      }
    } else {
      logger.warn("User {} is not authorized to join room {} (player1: {}, player2: {})",
          userId, roomId, room.getPlayer1Id(), room.getPlayer2Id());
      return false;
    }

    if (canJoin) {
      // 确保创建游戏状态
      if (room.getGameState() == null) {
        logger.info("Creating new game state for room {}", roomId);
        try {
          room.createNewGame();
          // 验证游戏状态是否创建成功
          if (room.getGameState() == null) {
            logger.error(
                "Failed to create game state for room {} - createNewGame() did not initialize gameState, trying manual creation",
                roomId);

            // 直接创建游戏状态并强制设置
            try {
              logger.info("Attempting to manually create and set game state for room {}", roomId);

              // 直接使用正确的构造函数创建游戏状态
              ChessGameState gameState = new ChessGameState(
                  java.util.UUID.randomUUID().toString(), // gameId
                  roomId // roomId
              );

              // 设置游戏状态为等待状态
              gameState.setStatus(GameStatus.WAITING);

              // 使用反射获取ChessRoom的gameState字段并设置
              java.lang.reflect.Field gameStateField = ChessRoom.class.getDeclaredField("gameState");
              gameStateField.setAccessible(true);
              gameStateField.set(room, gameState);

              logger.info("Successfully set game state manually for room {} with status {}",
                  roomId, gameState.getStatus());

              // 验证设置是否成功
              if (room.getGameState() != null) {
                logger.info("Game state verification successful - room.getGameState() is not null");
              } else {
                logger.error("Game state verification failed - room.getGameState() is still null after manual setting");
                return false;
              }

            } catch (NoSuchFieldException e) {
              logger.error("ChessRoom class does not have 'gameState' field: {}", e.getMessage());
              return false;
            } catch (IllegalAccessException e) {
              logger.error("Cannot access 'gameState' field in ChessRoom: {}", e.getMessage());
              return false;
            } catch (Exception manualEx) {
              logger.error("Manual game state creation failed for room {}: {}", roomId, manualEx.getMessage(),
                  manualEx);
              return false;
            }
          } else {
            logger.info("Game state created successfully for room {}", roomId);
          }
        } catch (Exception e) {
          logger.error("Exception while creating new game for room {}: {}", roomId, e.getMessage(), e);
          return false;
        }
      }

      // 添加详细的调试信息
      logger.info("About to call room.joinPlayer() for user {} in room {} with name {} and type HUMAN",
          userId, roomId, playerName);

      // 检查游戏状态
      ChessGameState gameState = room.getGameState();
      if (gameState != null) {
        logger.info("Game state exists - Red player: {}, Black player: {}, Status: {}",
            gameState.getRedPlayer() != null ? gameState.getRedPlayer().getUserId() : "null",
            gameState.getBlackPlayer() != null ? gameState.getBlackPlayer().getUserId() : "null",
            gameState.getStatus());
      } else {
        logger.error("Game state is still null for room {} after all creation attempts", roomId);
        return false;
      }

      // 加入玩家
      try {
        boolean joined = room.joinPlayer(userId, playerName, PlayerType.HUMAN);
        logger.info("User {} join result for room {}: {}", userId, roomId, joined);

        if (!joined) {
          // 获取更多失败信息
          logger.error("room.joinPlayer() failed for user {} in room {}. Room details:", userId, roomId);
          logger.error("  - Room player1Id: {}", room.getPlayer1Id());
          logger.error("  - Room player2Id: {}", room.getPlayer2Id());
          logger.error("  - Room status: {}", room.getStatus());

          // 重新获取游戏状态以确保最新信息
          ChessGameState currentGameState = room.getGameState();
          if (currentGameState != null) {
            logger.error("  - GameState red player: {}",
                currentGameState.getRedPlayer() != null
                    ? currentGameState.getRedPlayer().getUserId() + "(" + currentGameState.getRedPlayer().getName()
                        + ")"
                    : "null");
            logger.error("  - GameState black player: {}",
                currentGameState.getBlackPlayer() != null
                    ? currentGameState.getBlackPlayer().getUserId() + "(" + currentGameState.getBlackPlayer().getName()
                        + ")"
                    : "null");
            logger.error("  - GameState status: {}", currentGameState.getStatus());

            // 尝试手动添加玩家到游戏状态
            try {
              logger.info("Attempting to manually add player to game state for room {}", roomId);

              // 确定玩家颜色并创建Player对象
              Player player = null;
              if (room.getPlayer1Id() != null && room.getPlayer1Id().equals(userId)) {
                // 当前用户是player1，设置为红方
                if (currentGameState.getRedPlayer() == null) {
                  player = new Player(userId, playerName, PlayerType.HUMAN, PlayerColor.RED);
                  currentGameState.setRedPlayer(player);
                  logger.info("Manually set user {} as red player in room {}", userId, roomId);
                  joined = true;
                }
              } else if (room.getPlayer2Id() != null && room.getPlayer2Id().equals(userId)) {
                // 当前用户是player2，设置为黑方
                if (currentGameState.getBlackPlayer() == null) {
                  player = new Player(userId, playerName, PlayerType.HUMAN, PlayerColor.BLACK);
                  currentGameState.setBlackPlayer(player);
                  logger.info("Manually set user {} as black player in room {}", userId, roomId);
                  joined = true;
                }
              }

              if (joined) {
                logger.info("Successfully manually added player {} to game state in room {}", userId, roomId);
              }
            } catch (Exception manualPlayerEx) {
              logger.error("Failed to manually add player to game state for room {}: {}",
                  roomId, manualPlayerEx.getMessage(), manualPlayerEx);
            }
          } else {
            logger.error("  - GameState is null");
          }
        }

        if (joined) {
          // 成功加入后记录最终状态
          ChessGameState finalGameState = room.getGameState();
          if (finalGameState != null) {
            logger.info("Successfully joined room {}. Final state - Red: {}, Black: {}",
                roomId,
                finalGameState.getRedPlayer() != null ? finalGameState.getRedPlayer().getUserId() : "null",
                finalGameState.getBlackPlayer() != null ? finalGameState.getBlackPlayer().getUserId() : "null");
          }
        }

        return joined;
      } catch (Exception e) {
        logger.error("Exception in room.joinPlayer() for user {} in room {}: {}", userId, roomId, e.getMessage(), e);
        return false;
      }
    }

    return false;
  }

  /**
   * 使用反射创建游戏状态
   */
  private ChessGameState createGameStateWithReflection() {
    try {
      // 尝试不同的构造方法
      Class<ChessGameState> clazz = ChessGameState.class;

      // 首先尝试查找可用的构造函数
      java.lang.reflect.Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      logger.info("Available ChessGameState constructors:");
      for (java.lang.reflect.Constructor<?> constructor : constructors) {
        logger.info("  - Constructor with {} parameters: {}",
            constructor.getParameterCount(),
            java.util.Arrays.toString(constructor.getParameterTypes()));
      }

      // 尝试找到(String, String)构造函数
      for (java.lang.reflect.Constructor<?> constructor : constructors) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        if (paramTypes.length == 2 &&
            paramTypes[0] == String.class &&
            paramTypes[1] == String.class) {

          constructor.setAccessible(true);

          // 创建游戏状态，使用随机gameId和roomId
          String gameId = java.util.UUID.randomUUID().toString();
          String roomId = java.util.UUID.randomUUID().toString();

          ChessGameState gameState = (ChessGameState) constructor.newInstance(gameId, roomId);
          logger.info("Successfully created ChessGameState using (String, String) constructor");
          return gameState;
        }
      }

      logger.error("No suitable constructor found for ChessGameState");
      return null;
    } catch (Exception e) {
      logger.error("Failed to create ChessGameState using reflection: {}", e.getMessage(), e);
      return null;
    }
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
    logger.info("Surrender request - roomId: {}, userId: {}", roomId, userId);

    ChessRoom room = activeRooms.get(roomId);
    if (room == null) {
      logger.warn("Surrender failed - room not found: {}", roomId);
      return false;
    }

    if (room.getGameState() == null) {
      logger.warn("Surrender failed - game state is null for room: {}", roomId);
      return false;
    }

    ChessGameState gameState = room.getGameState();
    logger.info("Game status: {}", gameState.getStatus());

    // 检查游戏是否正在进行
    if (gameState.getStatus() != GameStatus.PLAYING) {
      logger.warn("Surrender failed - game is not playing. Status: {}", gameState.getStatus());
      return false;
    }

    // 检查玩家是否在游戏中（红方或黑方）
    boolean isValidPlayer = false;
    if (gameState.getRedPlayer() != null) {
      logger.info("Red player: userId={}, name={}",
          gameState.getRedPlayer().getUserId(), gameState.getRedPlayer().getName());
      if (gameState.getRedPlayer().getUserId() != null &&
          gameState.getRedPlayer().getUserId().equals(userId)) {
        isValidPlayer = true;
        logger.info("User {} is red player", userId);
      }
    } else {
      logger.info("Red player is null");
    }

    if (gameState.getBlackPlayer() != null) {
      logger.info("Black player: userId={}, name={}",
          gameState.getBlackPlayer().getUserId(), gameState.getBlackPlayer().getName());
      if (gameState.getBlackPlayer().getUserId() != null &&
          gameState.getBlackPlayer().getUserId().equals(userId)) {
        isValidPlayer = true;
        logger.info("User {} is black player", userId);
      }
    } else {
      logger.info("Black player is null");
    }

    if (isValidPlayer) {
      logger.info("User {} is valid player, executing surrender", userId);
      gameState.surrender();
      logger.info("Surrender executed successfully. New status: {}", gameState.getStatus());
      return true;
    } else {
      logger.warn("Surrender failed - user {} is not a valid player in room {}", userId, roomId);
      return false;
    }
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
