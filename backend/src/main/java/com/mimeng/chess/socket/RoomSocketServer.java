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

  @Autowired
  private JwtUtil jwtUtil;

  // 存储客户端信息 clientId -> userInfo
  private final Map<String, UserInfo> clientUsers = new ConcurrentHashMap<>();
  // 存储房间中的客户端 roomId -> Set<clientId>
  private final Map<String, Map<String, SocketIOClient>> roomClients = new ConcurrentHashMap<>();
  // 防重复认证 - 存储正在认证的客户端
  private final Map<String, Boolean> authenticatingClients = new ConcurrentHashMap<>();
  // 存储已认证的客户端
  private final Map<String, Boolean> authenticatedClients = new ConcurrentHashMap<>();

  // 用户信息类
  private static class UserInfo {
    Long userId;
    String userName;
    String roomId;
    boolean isAuthenticated;

    UserInfo(Long userId, String userName, String roomId) {
      this.userId = userId;
      this.userName = userName;
      this.roomId = roomId;
      this.isAuthenticated = false;
    }
  }

  @Override
  public void afterPropertiesSet() {
    // 连接监听器
    socketIOServer.addConnectListener(new ConnectListener() {
      @Override
      public void onConnect(SocketIOClient client) {
        String roomId = client.getHandshakeData().getSingleUrlParam("id");
        String clientId = client.getSessionId().toString();

        logger.info("Client {} connected to room {}", clientId, roomId);

        if (roomId == null) {
          logger.warn("Client {} connected without roomId, disconnecting", clientId);
          client.disconnect();
          return;
        }

        // 检查是否已经有相同用户的连接
        logger.info("Client {} is trying to authenticate in room {}", clientId, roomId);
        client.sendEvent("need_auth", "请发送token进行鉴权");
      }
    });

    // 鉴权事件 - 添加防重复认证
    socketIOServer.addEventListener("auth", Object.class, (client, data, ackSender) -> {
      String clientId = client.getSessionId().toString();

      try {
        // 检查是否已经认证
        if (authenticatedClients.containsKey(clientId)) {
          logger.warn("Client {} already authenticated, ignoring duplicate auth request", clientId);
          client.sendEvent("auth_success", "已认证");
          return;
        }

        // 检查是否正在认证
        if (authenticatingClients.putIfAbsent(clientId, true) != null) {
          logger.warn("Client {} is already authenticating, ignoring duplicate auth request", clientId);
          return;
        }

        try {
          String token = null;

          // 调试日志：打印接收到的数据类型和内容
          logger.info("Auth data received from client {} - Type: {}, Content: {}",
              clientId,
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
                  logger.info("Extracted token from JSON string: {}...",
                      token.substring(0, Math.min(20, token.length())));
                }
              } catch (Exception jsonEx) {
                logger.warn("Failed to parse JSON string: {}", jsonEx.getMessage());
              }
            } else {
              // 直接作为token使用
              token = dataStr;
              logger.info("Using data directly as token: {}...",
                  token.substring(0, Math.min(20, token.length())));
            }
          } else if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            Object tokenObj = map.get("token");
            if (tokenObj instanceof String) {
              token = (String) tokenObj;
              logger.info("Extracted token from Map: {}...",
                  token.substring(0, Math.min(20, token.length())));
            }
          }

          if (token == null || token.trim().isEmpty()) {
            logger.warn("Client {} attempted auth without valid token", clientId);
            client.sendEvent("auth_fail", "token缺失或无效");
            client.disconnect();
            return;
          }

          // 解析JWT token
          Claims claims;
          try {
            logger.info("Attempting to parse JWT token for client {}: {}...",
                clientId, token.substring(0, Math.min(20, token.length())));
            claims = jwtUtil.parseToken(token);
            logger.info("JWT token parsed successfully for client {}", clientId);
          } catch (Exception e) {
            logger.error("Failed to parse JWT token for client {}: {}",
                clientId, e.getMessage(), e);
            client.sendEvent("auth_fail", "token解析失败: " + e.getMessage());
            client.disconnect();
            return;
          }

          String userIdStr = claims.get("userId", String.class);
          String email = claims.get("email", String.class);
          String roomId = client.getHandshakeData().getSingleUrlParam("id");

          logger.info("Extracted from token for client {} - userId: {}, email: {}, roomId: {}",
              clientId, userIdStr, email, roomId);

          if (userIdStr == null || email == null) {
            logger.warn("Client {} auth with missing userId or email in token", clientId);
            client.sendEvent("auth_fail", "token中缺少必要信息");
            client.disconnect();
            return;
          }

          Long userId;
          try {
            userId = Long.valueOf(userIdStr);
          } catch (NumberFormatException e) {
            logger.warn("Client {} auth with invalid userId format: {}", clientId, userIdStr);
            client.sendEvent("auth_fail", "用户ID格式无效");
            client.disconnect();
            return;
          }

          // 检查是否有相同用户的其他连接
          for (Map.Entry<String, UserInfo> entry : clientUsers.entrySet()) {
            UserInfo existingUser = entry.getValue();
            if (existingUser.userId.equals(userId) && existingUser.roomId.equals(roomId)) {
              logger.warn("User {} already has an active connection {} in room {}, disconnecting new connection {}",
                  userId, entry.getKey(), roomId, clientId);
              client.sendEvent("auth_fail", "该用户已在房间中");
              client.disconnect();
              return;
            }
          } // 使用用户ID作为显示名称，确保唯一性
          String userName = "User" + userId;

          if (userName == null || roomId == null) {
            logger.warn("Client {} auth with incomplete data: userId={}, userName={}, roomId={}",
                clientId, userId, userName, roomId);
            client.sendEvent("auth_fail", "认证信息不完整");
            client.disconnect();
            return;
          }

          // 保存用户信息
          UserInfo userInfo = new UserInfo(userId, userName, roomId);
          clientUsers.put(clientId, userInfo);

          // 加入房间
          client.joinRoom(roomId);
          roomClients.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
              .put(clientId, client);

          // 加入游戏房间
          logger.info("Attempting to join game room - roomId: {}, userId: {}, userName: {}",
              roomId, userId, userName);

          boolean joined = gameManager.joinRoom(roomId, userId, userName);
          logger.info("GameManager.joinRoom result for user {} in room {}: {}",
              userId, roomId, joined);
          if (joined) {
            // 标记为已认证
            userInfo.isAuthenticated = true;
            authenticatedClients.put(clientId, true);

            logger.info("User {} ({}) successfully authenticated and joined room {}", userName, userId, roomId);
            client.sendEvent("auth_success", "鉴权成功");

            // 广播玩家加入事件（排除自己）
            broadcastToRoomExcept(roomId, clientId, "player_joined", Map.of(
                "userId", userId,
                "userName", userName,
                "message", userName + " 加入了房间"));

            // 发送当前游戏状态给所有人
            sendGameState(roomId);

            // 发送房间状态给所有人
            sendRoomStatus(roomId);
          } else {
            logger.error("User {} ({}) failed to join room {} - GameManager.joinRoom returned false",
                userName, userId, roomId);

            // 清理已保存的信息
            clientUsers.remove(clientId);
            Map<String, SocketIOClient> roomClientMap = roomClients.get(roomId);
            if (roomClientMap != null) {
              roomClientMap.remove(clientId);
            }

            client.sendEvent("auth_fail", "房间已满或其他原因导致加入失败");
            client.disconnect();
          }
        } finally {
          // 移除认证状态
          authenticatingClients.remove(clientId);
        }
      } catch (Exception e) {
        logger.error("Auth error for client {}: {}", clientId, e.getMessage(), e);
        authenticatingClients.remove(clientId);
        client.sendEvent("auth_fail", "认证处理异常: " + e.getMessage());
        client.disconnect();
      }
    });

    // 获取房间状态事件
    socketIOServer.addEventListener("get_room_state", Object.class, (client, data, ackSender) -> {
      try {
        String clientId = client.getSessionId().toString();
        UserInfo userInfo = clientUsers.get(clientId);

        if (userInfo == null || !userInfo.isAuthenticated) {
          logger.warn("Unauthenticated client {} requested room state", clientId);
          client.sendEvent("auth_fail", "未认证的连接");
          return;
        }

        logger.info("Client {} requested room state for room {}", clientId, userInfo.roomId);

        // 发送游戏状态给单个客户端
        sendGameStateToClient(client, userInfo.roomId);
        sendRoomStatusToClient(client, userInfo.roomId);

      } catch (Exception e) {
        logger.error("Error handling get_room_state: {}", e.getMessage(), e);
        client.sendEvent("error", "获取房间状态时发生错误");
      }
    }); // 玩家准备事件
    socketIOServer.addEventListener("player_ready", Object.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted player_ready", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        // 处理不同格式的数据
        Boolean ready = null;

        if (data instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) data;
          ready = (Boolean) map.get("ready");
        } else if (data instanceof String) {
          String dataStr = (String) data;
          // 尝试解析JSON字符串
          if (dataStr.trim().startsWith("{") && dataStr.trim().endsWith("}")) {
            try {
              String readyPattern = "\"ready\"\\s*:\\s*(true|false)";
              java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(readyPattern);
              java.util.regex.Matcher matcher = pattern.matcher(dataStr);
              if (matcher.find()) {
                ready = Boolean.valueOf(matcher.group(1));
              }
            } catch (Exception jsonEx) {
              logger.warn("Failed to parse ready JSON string: {}", jsonEx.getMessage());
            }
          }
        }

        if (ready == null) {
          ready = true;
        }
        boolean success = gameManager.playerReady(userInfo.roomId, userInfo.userId, ready);
        if (success) {
          logger.info("Player {} ({}) ready status changed to {} in room {}",
              userInfo.userName, userInfo.userId, ready, userInfo.roomId);

          // 广播玩家准备状态变更给所有人
          broadcastToRoom(userInfo.roomId, "player_ready_changed", Map.of(
              "userId", userInfo.userId,
              "userName", userInfo.userName,
              "ready", ready,
              "message", userInfo.userName + (ready ? " 已准备" : " 取消准备")));

          // 发送更新的游戏状态给所有人
          sendGameState(userInfo.roomId);

          // 发送房间状态给所有人
          sendRoomStatus(userInfo.roomId);

          // 检查是否可以开始游戏
          if (ready && gameManager.tryStartGame(userInfo.roomId)) {
            logger.info("Game started in room {}", userInfo.roomId);
            broadcastToRoom(userInfo.roomId, "game_started", Map.of(
                "message", "游戏开始！"));
          }
        } else {
          logger.warn("Failed to set ready status for player {} in room {}", userInfo.userId, userInfo.roomId);
          client.sendEvent("ready_failed", "设置准备状态失败");
        }
      } catch (Exception e) {
        logger.error("Error handling player_ready: {}", e.getMessage(), e);
        client.sendEvent("error", "处理准备状态时发生错误");
      }
    }); // 选择棋子事件
    socketIOServer.addEventListener("select_piece", Object.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted select_piece", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        Integer row = null;
        Integer col = null;

        // 处理不同格式的数据
        if (data instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) data;
          row = (Integer) map.get("row");
          col = (Integer) map.get("col");
        } else if (data instanceof String) {
          String dataStr = (String) data;
          if (dataStr.trim().startsWith("{") && dataStr.trim().endsWith("}")) {
            try {
              // 简单的JSON解析
              String rowPattern = "\"row\"\\s*:\\s*(\\d+)";
              String colPattern = "\"col\"\\s*:\\s*(\\d+)";

              java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(rowPattern);
              java.util.regex.Matcher matcher = pattern.matcher(dataStr);
              if (matcher.find()) {
                row = Integer.valueOf(matcher.group(1));
              }

              pattern = java.util.regex.Pattern.compile(colPattern);
              matcher = pattern.matcher(dataStr);
              if (matcher.find()) {
                col = Integer.valueOf(matcher.group(1));
              }
            } catch (Exception e) {
              logger.warn("Failed to parse select_piece JSON: {}", e.getMessage());
            }
          }
        }

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
    }); // 移动棋子事件
    socketIOServer.addEventListener("move_piece", Object.class, (client, data, ackSender) -> {
      try {
        UserInfo userInfo = clientUsers.get(client.getSessionId().toString());
        if (userInfo == null) {
          logger.warn("Unauthenticated client {} attempted move_piece", client.getSessionId());
          client.sendEvent("error", "未认证的连接");
          return;
        }

        Map<String, Object> from = null;
        Map<String, Object> to = null;

        // 处理不同格式的数据
        if (data instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) data;
          from = (Map<String, Object>) map.get("from");
          to = (Map<String, Object>) map.get("to");
        } else if (data instanceof String) {
          String dataStr = (String) data;
          if (dataStr.trim().startsWith("{") && dataStr.trim().endsWith("}")) {
            try {
              // 简单的JSON解析移动数据
              // 这是一个复杂的嵌套JSON，建议前端直接发送Map格式
              logger.warn("Received JSON string format for move_piece, please use Map format from frontend");
              client.sendEvent("move_failed", "请使用正确的数据格式");
              return;
            } catch (Exception e) {
              logger.warn("Failed to parse move_piece JSON: {}", e.getMessage());
              client.sendEvent("move_failed", "JSON解析失败");
              return;
            }
          }
        }

        if (from == null || to == null ||
            from.get("row") == null || from.get("col") == null ||
            to.get("row") == null || to.get("col") == null) {
          logger.warn("Invalid move_piece data from user {} in room {}", userInfo.userId, userInfo.roomId);
          client.sendEvent("move_failed", "移动参数无效");
          return;
        }

        Position fromPos = new Position((Integer) from.get("row"), (Integer) from.get("col"));
        Position toPos = new Position((Integer) to.get("row"), (Integer) to.get("col"));

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
          sendGameState(userInfo.roomId); // 检查游戏是否结束
          ChessGameState gameState = gameManager.getGameState(userInfo.roomId);
          if (gameState != null && gameState.getStatus() != GameStatus.PLAYING) {
            logger.info("Game ended in room {} with status {}", userInfo.roomId, gameState.getStatus());
            broadcastToRoom(userInfo.roomId, "game_ended", Map.of(
                "status", gameState.getStatus().toString(),
                "message", gameState.getGameStatusSummary()));

            // 游戏结束后，延迟10秒后通知用户退出并删除房间
            handleGameEnded(userInfo.roomId);
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

          // 投降后游戏结束，通知用户退出并删除房间
          handleGameEnded(userInfo.roomId);
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

          // 清理认证状态
          authenticatingClients.remove(clientId);
          authenticatedClients.remove(clientId);

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
            } // 只有已认证的用户才需要离开游戏房间
            if (userInfo.isAuthenticated) {
              gameManager.leaveRoom(userInfo.roomId, userInfo.userId);

              // 广播玩家离开事件
              broadcastToRoom(userInfo.roomId, "player_left", Map.of(
                  "userId", userInfo.userId,
                  "userName", userInfo.userName,
                  "message", userInfo.userName + " 离开了房间"));

              // 发送更新的游戏状态和房间状态给剩余玩家
              sendGameState(userInfo.roomId);
              sendRoomStatus(userInfo.roomId);
            }
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
      logger.debug("Broadcasted event '{}' to {} clients in room {}", event, clients.size(), roomId);
    }
  }

  /**
   * 向房间内除了指定客户端外的所有客户端广播消息
   */
  private void broadcastToRoomExcept(String roomId, String excludeClientId, String event, Object data) {
    Map<String, SocketIOClient> clients = roomClients.get(roomId);
    if (clients != null) {
      int broadcastCount = 0;
      for (Map.Entry<String, SocketIOClient> entry : clients.entrySet()) {
        if (!entry.getKey().equals(excludeClientId)) {
          entry.getValue().sendEvent(event, data);
          broadcastCount++;
        }
      }
      logger.debug("Broadcasted event '{}' to {} clients in room {} (excluded {})",
          event, broadcastCount, roomId, excludeClientId);
    }
  }

  /**
   * 发送房间状态给房间内所有客户端
   */
  private void sendRoomStatus(String roomId) {
    Map<String, SocketIOClient> clients = roomClients.get(roomId);
    if (clients != null) {
      for (SocketIOClient client : clients.values()) {
        sendRoomStatusToClient(client, roomId);
      }
    }
  }

  /**
   * 发送房间状态给指定客户端
   */
  private void sendRoomStatusToClient(SocketIOClient client, String roomId) {
    try {
      ChessGameState gameState = gameManager.getGameState(roomId);
      if (gameState == null) {
        logger.warn("No game state found for room {}", roomId);
        client.sendEvent("room_status", Map.of(
            "status", "waiting",
            "message", "等待游戏状态初始化"));
        return;
      }

      Map<String, Object> roomStatus = new HashMap<>();
      roomStatus.put("status", gameState.getStatus().toString());
      roomStatus.put("message", gameState.getGameStatusSummary());

      // 玩家状态信息
      Map<String, Object> players = new HashMap<>();
      if (gameState.getRedPlayer() != null) {
        Player redPlayer = gameState.getRedPlayer();
        players.put("red", Map.of(
            "userId", redPlayer.getUserId(),
            "name", redPlayer.getName(),
            "ready", redPlayer.isReady(),
            "online", redPlayer.isOnline(),
            "color", "RED"));
      }

      if (gameState.getBlackPlayer() != null) {
        Player blackPlayer = gameState.getBlackPlayer();
        players.put("black", Map.of(
            "userId", blackPlayer.getUserId(),
            "name", blackPlayer.getName(),
            "ready", blackPlayer.isReady(),
            "online", blackPlayer.isOnline(),
            "color", "BLACK"));
      }

      roomStatus.put("players", players);
      roomStatus.put("waitingForPlayers", gameState.getRedPlayer() == null || gameState.getBlackPlayer() == null);

      client.sendEvent("room_status", roomStatus);
      logger.debug("Room status sent to client in room {}", roomId);

    } catch (Exception e) {
      logger.error("Error sending room status to client in room {}: {}", roomId, e.getMessage(), e);
    }
  }

  /**
   * 发送游戏状态给房间内所有客户端
   */
  private void sendGameState(String roomId) {
    Map<String, SocketIOClient> clients = roomClients.get(roomId);
    if (clients != null) {
      for (SocketIOClient client : clients.values()) {
        sendGameStateToClient(client, roomId);
      }
    }
  }

  /**
   * 发送游戏状态给指定客户端
   */
  private void sendGameStateToClient(SocketIOClient client, String roomId) {
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
            "ready", gameState.getRedPlayer().isReady(),
            "online", gameState.getRedPlayer().isOnline(),
            "color", "RED"));
      }

      if (gameState.getBlackPlayer() != null) {
        stateData.put("blackPlayer", Map.of(
            "userId", gameState.getBlackPlayer().getUserId(),
            "name", gameState.getBlackPlayer().getName(),
            "ready", gameState.getBlackPlayer().isReady(),
            "online", gameState.getBlackPlayer().isOnline(),
            "color", "BLACK"));
      }

      // 棋盘状态
      ChessBoard board = gameState.getBoard();
      if (board != null) {
        stateData.put("boardState", serializeBoardState(board));
      }

      client.sendEvent("game_state", stateData);
      logger.debug("Game state sent to client in room {}", roomId);

    } catch (Exception e) {
      logger.error("Error sending game state to client in room {}: {}", roomId, e.getMessage(), e);
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

  /**
   * 处理游戏结束后的清理工作
   */
  private void handleGameEnded(String roomId) {
    // 创建定时任务，延迟10秒后通知用户退出并删除房间
    java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors
        .newSingleThreadScheduledExecutor();

    scheduler.schedule(() -> {
      try {
        logger.info("Handling game ended for room {}", roomId);
        // 通知房间内所有用户游戏结束，需要退出
        broadcastToRoom(roomId, "room_closing", Map.of(
            "message", "游戏已结束，房间将在30秒后关闭",
            "countdown", 30));

        // 再延迟30秒后强制断开连接并删除房间
        scheduler.schedule(() -> {
          try {
            logger.info("Force closing room {}", roomId);

            // 通知所有用户房间关闭
            broadcastToRoom(roomId, "room_closed", Map.of(
                "message", "房间已关闭"));

            // 断开房间内所有客户端连接
            Map<String, SocketIOClient> clients = roomClients.get(roomId);
            if (clients != null) {
              for (SocketIOClient client : clients.values()) {
                try {
                  client.sendEvent("forced_disconnect", "房间已关闭，连接即将断开");
                  // 短暂延迟后断开连接，确保消息能发送
                  java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                      .schedule(() -> client.disconnect(), 2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                  logger.error("Error disconnecting client: {}", e.getMessage());
                }
              }
            }

            // 清理客户端信息
            if (clients != null) {
              for (String clientId : clients.keySet()) {
                clientUsers.remove(clientId);
                authenticatedClients.remove(clientId);
              }
            }

            // 清理房间信息
            roomClients.remove(roomId);

            // 从游戏管理器中删除房间
            gameManager.removeRoom(roomId);

            logger.info("Room {} has been completely cleaned up", roomId);

          } catch (Exception e) {
            logger.error("Error during room cleanup for {}: {}", roomId, e.getMessage(), e);
          } finally {
            scheduler.shutdown();
          }
        }, 30, java.util.concurrent.TimeUnit.SECONDS);

      } catch (Exception e) {
        logger.error("Error handling game ended for room {}: {}", roomId, e.getMessage(), e);
      }
    }, 10, java.util.concurrent.TimeUnit.SECONDS);
  }
}
