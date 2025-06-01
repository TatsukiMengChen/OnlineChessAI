package com.mimeng.chess.socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mimeng.chess.BuildConfig;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URISyntaxException;

/**
 * Socket.IO 连接管理器
 */
public class SocketManager {
  private Socket socket;
  private SocketEventListener listener;
  private boolean isConnected = false;
  private String roomId;
  private String userToken;

  public SocketManager(String roomId, String userToken, SocketEventListener listener) {
    this.roomId = roomId;
    this.userToken = userToken;
    this.listener = listener;
  }

  /**
   * 建立 Socket 连接
   */
  public void connect() {
    try {
      IO.Options options = new IO.Options();
      options.query = "id=" + roomId; // 修改参数名从 roomId 到 id，匹配后端期望

      socket = IO.socket(BuildConfig.WS_URL, options);
      setupEventListeners();
      socket.connect();

    } catch (URISyntaxException e) {
      if (listener != null) {
        listener.onError("连接地址错误");
      }
    }
  }

  /**
   * 断开连接
   */
  public void disconnect() {
    if (socket != null) {
      socket.emit("leave_room");
      socket.disconnect();
      socket.off();
      socket = null;
    }
    isConnected = false;
  }

  /**
   * 发送玩家准备状态
   */
  public void sendPlayerReady(boolean ready) {
    if (isConnected && socket != null) {
      JsonObject readyData = new JsonObject();
      readyData.addProperty("ready", ready);
      socket.emit("player_ready", readyData);
    }
  }

  /**
   * 请求房间状态
   */
  public void requestRoomState() {
    if (isConnected && socket != null) {
      socket.emit("get_room_state");
    }
  }

  /**
   * 获取连接状态
   */
  public boolean isConnected() {
    return isConnected;
  }

  /**
   * 设置事件监听器
   */
  private void setupEventListeners() {
    if (socket == null)
      return;

    // 连接事件
    socket.on(Socket.EVENT_CONNECT, onConnect);
    socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
    socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);

    // 认证事件
    socket.on("need_auth", onNeedAuth);
    socket.on("auth_success", onAuthSuccess);
    socket.on("auth_fail", onAuthFail);

    // 房间事件
    socket.on("room_state", onRoomState);
    socket.on("user_entered_room", onUserEntered);
    socket.on("user_left_room", onUserLeft);

    // 游戏事件
    socket.on("player_ready_changed", onPlayerReadyChanged);
    socket.on("game_started", onGameStarted);
  }

  // 事件监听器实现
  private Emitter.Listener onConnect = args -> {
    isConnected = true;
    if (listener != null) {
      listener.onConnectionChanged(true);
    }
  };

  private Emitter.Listener onDisconnect = args -> {
    isConnected = false;
    if (listener != null) {
      listener.onConnectionChanged(false);
    }
  };

  private Emitter.Listener onConnectError = args -> {
    if (listener != null) {
      listener.onError("Socket连接失败");
    }
  };

  private Emitter.Listener onNeedAuth = args -> {
    JsonObject authData = new JsonObject();
    authData.addProperty("token", userToken);
    socket.emit("auth", authData);
  };

  private Emitter.Listener onAuthSuccess = args -> {
    if (listener != null) {
      listener.onAuthSuccess();
    }
    // 自动请求房间状态
    requestRoomState();
  };

  private Emitter.Listener onAuthFail = args -> {
    String message = "认证失败";
    try {
      if (args.length > 0) {
        JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
        if (data.has("message")) {
          message = data.get("message").getAsString();
        }
      }
    } catch (Exception e) {
      // 使用默认消息
    }

    if (listener != null) {
      listener.onAuthFail(message);
    }
  };

  private Emitter.Listener onRoomState = args -> {
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (listener != null) {
        listener.onRoomStateUpdated(data);
      }
    } catch (Exception e) {
      if (listener != null) {
        listener.onError("房间状态解析失败");
      }
    }
  };

  private Emitter.Listener onUserEntered = args -> {
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      String username = data.getAsJsonObject("user").get("username").getAsString();
      if (listener != null) {
        listener.onUserEntered(username);
      }
      requestRoomState();
    } catch (Exception e) {
      // 忽略解析错误
    }
  };

  private Emitter.Listener onUserLeft = args -> {
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      String username = data.get("username").getAsString();
      if (listener != null) {
        listener.onUserLeft(username);
      }
      requestRoomState();
    } catch (Exception e) {
      // 忽略解析错误
    }
  };

  private Emitter.Listener onPlayerReadyChanged = args -> {
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      String username = data.get("username").getAsString();
      boolean ready = data.get("ready").getAsBoolean();
      if (listener != null) {
        listener.onPlayerReadyChanged(username, ready);
      }
      requestRoomState();
    } catch (Exception e) {
      // 忽略解析错误
    }
  };

  private Emitter.Listener onGameStarted = args -> {
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (listener != null) {
        listener.onGameStarted(data);
      }
    } catch (Exception e) {
      if (listener != null) {
        listener.onError("游戏开始数据解析失败");
      }
    }
  };
}
