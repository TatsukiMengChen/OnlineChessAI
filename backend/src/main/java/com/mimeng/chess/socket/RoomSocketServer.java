package com.mimeng.chess.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.SocketIOClient;
import io.jsonwebtoken.Claims;
import com.mimeng.chess.util.JwtUtil;
import com.mimeng.chess.entity.chess.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomSocketServer implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(RoomSocketServer.class);
  @Autowired
  private SocketIOServer socketIOServer;

  @Autowired
  private GameManager gameManager;

  @Autowired
  private com.mimeng.chess.service.UserService userService;

  // 存储客户端信息 clientId -> userInfo
  private final Map<String, UserInfo> clientUsers = new ConcurrentHashMap<>();
  // 存储房间中的客户端 roomId -> Set<clientId>
  private final Map<String, Map<String, SocketIOClient>> roomClients = new ConcurrentHashMap<>();

  // 用户信息类
  private static class UserInfo {
    Long userId;
    String userName;
    String roomId;

    UserInfo(Long userId, String userName, String roomId) {
      this.userId = userId;
      this.userName = userName;
      this.roomId = roomId;
    }
  }

  @Override
  public void afterPropertiesSet() {
    // 连接监听器
    socketIOServer.addConnectListener(new ConnectListener() {
      @Override
      public void onConnect(SocketIOClient client) {
        String roomId = client.getHandshakeData().getSingleUrlParam("id");
        logger.info("Client {} connected to room {}", client.getSessionId(), roomId);

        if (roomId == null) {
          client.disconnect();
          return;
        }
        logger.info("Client {} is trying to authenticate in room {}", client.getSessionId(), roomId);
        client.sendEvent("need_auth", "请发送token进行鉴权");
      }
    });

    // 鉴权事件 - 修复的核心部分
    socketIOServer.addEventListener("auth", Object.class, (client, data, ackSender) -> {
      try {
        String token = null;

        // 调试日志：打印接收到的数据类型和内容
        logger.debug("Auth data received - Type: {}, Content: {}",
            data != null ? data.getClass().getSimpleName() : "null", data);

        // 处理不同格式的token数据
        if (data instanceof String) {
          String dataStr = (String) data;
          // 检查是否是JSON格式的字符串
          if (dataStr.trim().startsWith("{") && dataStr.trim().endsWith("}")) {
            // 尝试手动解析JSON字符串
            try {
              // 简单的JSON解析，查找token字段
              String tokenPattern = "\"token\"\\s*:\\s*\"([^\"]+)\"";
              java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(tokenPattern);
              java.util.regex.Matcher matcher = pattern.matcher(dataStr);
              if (matcher.find()) {
                token = matcher.group(1);
                logger.debug("Extracted token from JSON string: {}",
                    token.substring(0, Math.min(20, token.length())) + "...");
              }
            } catch (Exception jsonEx) {
              logger.warn("Failed to parse JSON string: {}", jsonEx.getMessage());
            }
          } else {
            // 直接作为token使用
            token = dataStr;
          }
        } else if (data instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) data;
          Object tokenObj = map.get("token");
          if (tokenObj instanceof String) {
            token = (String) tokenObj;
            logger.debug("Extracted token from Map: {}", token.substring(0, Math.min(20, token.length())) + "...");
          }
        }

        if (token == null || token.trim().isEmpty()) {
          logger.warn("Client {} attempted auth without valid token", client.getSessionId());
          client.sendEvent("auth_fail", "token缺失或无效");
          client.disconnect();
          return;
        } // 解析JWT token
        Claims claims;
        try {
          logger.debug("Attempting to parse JWT token: {}", token.substring(0, Math.min(20, token.length())) + "...");
          claims = JwtUtil.parseToken(token);
          logger.debug("JWT token parsed successfully");
        } catch (Exception e) {
          logger.error("Failed to parse JWT token: {}", e.getMessage(), e);
          client.sendEvent("auth_fail", "token解析失败: " + e.getMessage());
          client.disconnect();
          return;
        }

        String userIdStr = claims.get("userId", String.class);
        String email = claims.get("email", String.class);
        String roomId = client.getHandshakeData().getSingleUrlParam("id");

        logger.debug("Extracted from token - userId: {}, email: {}, roomId: {}", userIdStr, email, roomId);

        if (userIdStr == null || email == null) {
          logger.warn("Client {} auth with missing userId or email in token", client.getSessionId());
          client.sendEvent("auth_fail", "token中缺少必要信息");
          client.disconnect();
          return;
        }

        Long userId = Long.valueOf(userIdStr);
        // 从email中提取用户名作为显示名称
        String userName = email.split("@")[0];

        if (userId == null || userName == null || roomId == null) {
          logger.warn("Client {} auth with incomplete data: userId={}, userName={}, roomId={}",
              client.getSessionId(), userId, userName, roomId);
          client.sendEvent("auth_fail", "认证信息不完整");
          client.disconnect();
          return;
        }

        // 保存用户信息
        UserInfo userInfo = new UserInfo(userId, userName, roomId);
        clientUsers.put(client.getSessionId().toString(), userInfo);

        // 加入房间
        client.joinRoom(roomId);
        roomClients.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
            .put(client.getSessionId().toString(), client);

        // 加入游戏房间
        boolean joined = gameManager.joinRoom(roomId, userId, userName);
        if (joined) {
          logger.info("User {} ({}) successfully authenticated and joined room {}", userName, userId, roomId);
          client.sendEvent("auth_success", "鉴权成功");

          // 广播玩家加入
          broadcastToRoom(roomId, "player_joined", Map.of(
              "userId", userId,
              "userName", userName));

          // 发送当前游戏状态
          sendGameState(roomId);
        } else {
          logger.warn("User {} ({}) failed to join room {}", userName, userId, roomId);
          client.sendEvent("auth_fail", "加入房间失败");
          client.disconnect();
        }
      } catch (Exception e) {
        logger.error("Auth error for client {}: {}", client.getSessionId(), e.getMessage(), e);
        client.sendEvent("auth_fail", "token无效");
        client.disconnect();
      }
    });

    // 玩家准备事件
    socketIOServer.addEventListener("player_ready", Map.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted player_ready", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        Boolean ready = (Boolean) data.get("ready");
        if (ready == null) {
          ready = true;
        }

        boolean success = gameManager.playerReady(userInfo.roomId, userInfo.userId, ready);
        if (success) {
          logger.info("Player {} ({}) ready status changed to {} in room {}",
              userInfo.userName, userInfo.userId, ready, userInfo.roomId);

          // 广播玩家准备状态
          broadcastToRoom(userInfo.roomId, "player_ready_changed", Map.of(
              "userId", userInfo.userId,
              "ready", ready));

          // 检查是否可以开始游戏
          if (ready && gameManager.tryStartGame(userInfo.roomId)) {
            logger.info("Game started in room {}", userInfo.roomId);
            broadcastToRoom(userInfo.roomId, "game_started", Map.of(
                "message", "游戏开始！"));
          }

          // 发送更新的游戏状态
          sendGameState(userInfo.roomId);
        } else {
          logger.warn("Failed to set ready status for player {} in room {}", userInfo.userId, userInfo.roomId);
          client.sendEvent("ready_failed", "设置准备状态失败");
        }
      } catch (Exception e) {
        logger.error("Error handling player_ready: {}", e.getMessage(), e);
        client.sendEvent("error", "处理准备状态时发生错误");
      }
    });

    // 选择棋子事件
    socketIOServer.addEventListener("select_piece", Map.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted select_piece", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        Integer row = (Integer) data.get("row");
        Integer col = (Integer) data.get("col");
        if (row == null || col == null) {
          logger.warn("Invalid select_piece data from user {} in room {}: row={}, col={}",
              userInfo.userId, userInfo.roomId, row, col);
          client.sendEvent("select_failed", "坐标参数无效");
          return;
        }

        Position position = new Position(row, col);
        boolean success = gameManager.playerSelectPiece(userInfo.roomId, userInfo.userId, position);

        if (success) {
          logger.debug("Player {} selected piece at ({}, {}) in room {}",
              userInfo.userId, row, col, userInfo.roomId);

          // 广播棋子选择
          broadcastToRoom(userInfo.roomId, "piece_selected", Map.of(
              "userId", userInfo.userId,
              "position", Map.of("row", row, "col", col)));

          // 发送可移动位置
          ChessGameState gameState = gameManager.getGameState(userInfo.roomId);
          if (gameState != null && gameState.getSelectedPiece() != null) {
            client.sendEvent("available_moves", Map.of(
                "moves", gameState.getAvailableMoves().stream()
                    .map(pos -> Map.of("row", pos.getRow(), "col", pos.getCol()))
                    .toList()));
          }
        } else {
          logger.debug("Player {} failed to select piece at ({}, {}) in room {}",
              userInfo.userId, row, col, userInfo.roomId);
          client.sendEvent("select_failed", "选择棋子失败");
        }
      } catch (Exception e) {
        logger.error("Error handling select_piece: {}", e.getMessage(), e);
        client.sendEvent("error", "选择棋子时发生错误");
      }
    });

    // 移动棋子事件
    socketIOServer.addEventListener("move_piece", Map.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted move_piece", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Integer> from = (Map<String, Integer>) data.get("from");
        @SuppressWarnings("unchecked")
        Map<String, Integer> to = (Map<String, Integer>) data.get("to");

        if (from == null || to == null ||
            from.get("row") == null || from.get("col") == null ||
            to.get("row") == null || to.get("col") == null) {
          logger.warn("Invalid move_piece data from user {} in room {}", userInfo.userId, userInfo.roomId);
          client.sendEvent("move_failed", "移动参数无效");
          return;
        }

        Position fromPos = new Position(from.get("row"), from.get("col"));
        Position toPos = new Position(to.get("row"), to.get("col"));

        boolean success = gameManager.playerMove(userInfo.roomId, userInfo.userId, fromPos, toPos);

        if (success) {
          logger.info("Player {} moved piece from ({}, {}) to ({}, {}) in room {}",
              userInfo.userId, fromPos.getRow(), fromPos.getCol(),
              toPos.getRow(), toPos.getCol(), userInfo.roomId);

          // 广播移动
          broadcastToRoom(userInfo.roomId, "piece_moved", Map.of(
              "userId", userInfo.userId,
              "from", Map.of("row", fromPos.getRow(), "col", fromPos.getCol()),
              "to", Map.of("row", toPos.getRow(), "col", toPos.getCol())));

          // 发送更新的游戏状态
          sendGameState(userInfo.roomId);

          // 检查游戏是否结束
          ChessGameState gameState = gameManager.getGameState(userInfo.roomId);
          if (gameState != null && gameState.getStatus() != GameStatus.PLAYING) {
            logger.info("Game ended in room {} with status {}", userInfo.roomId, gameState.getStatus());
            broadcastToRoom(userInfo.roomId, "game_ended", Map.of(
                "status", gameState.getStatus().toString(),
                "message", gameState.getGameStatusSummary()));
          }
        } else {
          logger.debug("Player {} failed to move piece from ({}, {}) to ({}, {}) in room {}",
              userInfo.userId, fromPos.getRow(), fromPos.getCol(),
              toPos.getRow(), toPos.getCol(), userInfo.roomId);
          client.sendEvent("move_failed", "移动失败");
        }
      } catch (Exception e) {
        logger.error("Error handling move_piece: {}", e.getMessage(), e);
        client.sendEvent("error", "移动棋子时发生错误");
      }
    });

    // 投降事件
    socketIOServer.addEventListener("surrender", Object.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted surrender", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        boolean success = gameManager.surrender(userInfo.roomId, userInfo.userId);
        if (success) {
          logger.info("Player {} ({}) surrendered in room {}", userInfo.userName, userInfo.userId, userInfo.roomId);
          broadcastToRoom(userInfo.roomId, "player_surrendered", Map.of(
              "userId", userInfo.userId,
              "userName", userInfo.userName));

          sendGameState(userInfo.roomId);
        } else {
          logger.warn("Player {} failed to surrender in room {}", userInfo.userId, userInfo.roomId);
          client.sendEvent("surrender_failed", "投降失败");
        }
      } catch (Exception e) {
        logger.error("Error handling surrender: {}", e.getMessage(), e);
        client.sendEvent("error", "投降时发生错误");
      }
    });

    // 悔棋事件
    socketIOServer.addEventListener("undo_move", Object.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted undo_move", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        boolean success = gameManager.undoMove(userInfo.roomId, userInfo.userId);
        if (success) {
          logger.info("Player {} requested undo in room {}", userInfo.userId, userInfo.roomId);
          broadcastToRoom(userInfo.roomId, "move_undone", Map.of(
              "userId", userInfo.userId));

          sendGameState(userInfo.roomId);
        } else {
          logger.debug("Player {} failed to undo move in room {}", userInfo.userId, userInfo.roomId);
          client.sendEvent("undo_failed", "悔棋失败");
        }
      } catch (Exception e) {
        logger.error("Error handling undo_move: {}", e.getMessage(), e);
        client.sendEvent("error", "悔棋时发生错误");
      }
    });

    // 离开房间事件
    socketIOServer.addEventListener("leave_room", Object.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo != null) {
          logger.info("Player {} requested to leave room {}", userInfo.userId, userInfo.roomId);
          // 断开连接会自动触发离开房间的逻辑
          client.disconnect();
        }
      } catch (Exception e) {
        logger.error("Error handling leave_room: {}", e.getMessage(), e);
      }
    });

    // 断开连接监听器
    socketIOServer.addDisconnectListener(new DisconnectListener() {
      @Override
      public void onDisconnect(SocketIOClient client) {
        try {
          String clientId = client.getSessionId().toString();
          UserInfo userInfo = clientUsers.remove(clientId);

          if (userInfo != null) {
            logger.info("User {} ({}) disconnected from room {}", userInfo.userName, userInfo.userId, userInfo.roomId);

            // 从房间中移除客户端
            Map<String, SocketIOClient> roomClientMap = roomClients.get(userInfo.roomId);
            if (roomClientMap != null) {
              roomClientMap.remove(clientId);
              if (roomClientMap.isEmpty()) {
                roomClients.remove(userInfo.roomId);
                logger.info("Room {} now empty, removing from active rooms", userInfo.roomId);
              }
            }

            // 玩家离开游戏
            gameManager.leaveRoom(userInfo.roomId, userInfo.userId);

            // 广播玩家离开
            broadcastToRoom(userInfo.roomId, "player_left", Map.of(
                "userId", userInfo.userId,
                "userName", userInfo.userName));
          } else {
            logger.debug("Unknown client {} disconnected", clientId);
          }
        } catch (Exception e) {
          logger.error("Error handling client disconnect: {}", e.getMessage(), e);
        }
      }
    });

    socketIOServer.start();
  }

  /**
   * 向房间内所有客户端广播消息
   */
  private void broadcastToRoom(String roomId, String event, Object data) {
    Map<String, SocketIOClient> clients = roomClients.get(roomId);
    if (clients != null) {
      for (SocketIOClient client : clients.values()) {
        client.sendEvent(event, data);
      }
    }
  }

  /**
   * 发送游戏状态给房间内所有客户端
   */
  private void sendGameState(String roomId) {
    try {
      ChessGameState gameState = gameManager.getGameState(roomId);
      if (gameState == null) {
        logger.warn("No game state found for room {}", roomId);
        return;
      }

      Map<String, Object> stateData = new HashMap<>();
      stateData.put("status", gameState.getStatus().toString());
      stateData.put("currentPlayer", gameState.getCurrentPlayer().toString());

      // 玩家信息
      if (gameState.getRedPlayer() != null) {
        stateData.put("redPlayer", Map.of(
            "userId", gameState.getRedPlayer().getUserId(),
            "name", gameState.getRedPlayer().getName(),
            "ready", gameState.getRedPlayer().isReady()));
      }

      if (gameState.getBlackPlayer() != null) {
        stateData.put("blackPlayer", Map.of(
            "userId", gameState.getBlackPlayer().getUserId(),
            "name", gameState.getBlackPlayer().getName(),
            "ready", gameState.getBlackPlayer().isReady()));
      }

      // 棋盘状态
      ChessBoard board = gameState.getBoard();
      if (board != null) {
        stateData.put("boardState", serializeBoardState(board));
      }

      broadcastToRoom(roomId, "game_state", stateData);
      logger.debug("Game state sent to room {}", roomId);

    } catch (Exception e) {
      logger.error("Error sending game state to room {}: {}", roomId, e.getMessage(), e);
    }
  }

  /**
   * 序列化棋盘状态为JSON格式
   */
  private Map<String, Object> serializeBoardState(ChessBoard board) {
    Map<String, Object> boardData = new HashMap<>();

    // 创建一个10x9的数组表示棋盘
    String[][] boardArray = new String[10][9];

    for (int row = 0; row < 10; row++) {
      for (int col = 0; col < 9; col++) {
        Position pos = new Position(row, col);
        ChessPiece piece = board.getPieceAt(pos);

        if (piece != null) {
          // 格式：颜色_棋子类型，例如 "red_king", "black_pawn"
          String color = piece.getColor().toString().toLowerCase();
          String type = piece.getClass().getSimpleName().toLowerCase();
          boardArray[row][col] = color + "_" + type;
        } else {
          boardArray[row][col] = null;
        }
      }
    }

    boardData.put("pieces", boardArray);
    return boardData;
  }
}
