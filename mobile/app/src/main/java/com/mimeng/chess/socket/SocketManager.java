package com.mimeng.chess.socket;

import android.content.Context;
import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mimeng.chess.BuildConfig;
import com.mimeng.chess.utils.AuthManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URISyntaxException;

/**
 * Socket.IO 连接管理器 - 单例模式
 */
public class SocketManager {
  private static final String TAG = "SocketManager";
  private static volatile SocketManager instance;
  private Socket socket;
  private SocketEventListener currentListener;
  private boolean isConnected = false;
  private String currentRoomId;
  private Context appContext;

  // 防抖相关
  private long lastAuthTime = 0;
  private long lastRoomStateRequestTime = 0;
  private static final long DEBOUNCE_DELAY = 500; // 缩短到500毫秒
  private boolean hasShownInitialError = false;
  private boolean isAuthenticated = false;
  private boolean isConnecting = false;
  private boolean isAuthenticating = false;

  private SocketManager(Context context) {
    this.appContext = context.getApplicationContext();
  }

  public static synchronized SocketManager getInstance(Context context) {
    if (instance == null) {
      instance = new SocketManager(context);
    }
    return instance;
  }

  /**
   * 注册监听器并设置房间上下文
   */
  public void registerListener(SocketEventListener listener, String roomId) {
    Log.d(TAG, "注册监听器: " + listener.getClass().getSimpleName() + " for room: " + roomId);
    this.currentListener = listener;
    this.currentRoomId = roomId;

    // 如果还没有连接，则建立连接
    if (!isConnected && !isConnecting) {
      Log.d(TAG, "Socket未连接，开始连接");
      connect();
    } else if (isConnected && !isAuthenticated && !isAuthenticating) {
      Log.d(TAG, "Socket已连接但未认证，开始认证");
      sendAuthWithDebounce();
    } else if (isConnected && isAuthenticated) {
      Log.d(TAG, "Socket已连接且已认证，通知监听器当前状态");
      // 通知新监听器当前状态
      if (currentListener != null) {
        currentListener.onConnectionChanged(true);
        currentListener.onAuthSuccess();
      }
      // 请求当前房间状态
      requestRoomState();
    }
  }

  /**
   * 取消注册监听器
   */
  public void unregisterListener(SocketEventListener listener) {
    if (this.currentListener == listener) {
      Log.d(TAG, "取消注册监听器: " + listener.getClass().getSimpleName());
      this.currentListener = null;
      // 注意：不要在这里断开Socket连接，因为是共享的
    }
  }

  /**
   * 建立 Socket 连接
   */
  public void connect() {
    if (isConnecting || isConnected) {
      Log.d(TAG, "Socket 正在连接或已连接，跳过连接请求");
      return;
    }

    try {
      // 如果之前有socket实例，先清理
      if (socket != null) {
        socket.off();
        socket.disconnect();
      }

      IO.Options options = new IO.Options();
      if (currentRoomId != null) {
        options.query = "id=" + currentRoomId; // 修改参数名从 roomId 到 id，匹配后端期望
      }

      Log.d(TAG, "开始连接 Socket.IO 服务器: " + BuildConfig.WS_URL + " with roomId: " + currentRoomId);

      isConnecting = true;
      socket = IO.socket(BuildConfig.WS_URL, options);
      setupEventListeners(); // 在连接前设置监听器
      socket.connect();

    } catch (URISyntaxException e) {
      Log.e(TAG, "连接地址错误: " + e.getMessage());
      isConnecting = false;
      if (currentListener != null) {
        currentListener.onError("连接地址错误");
      }
    }
  }

  /**
   * 断开连接
   */
  public void disconnect() {
    if (socket != null) {
      Log.d(TAG, "发送消息: leave_room");
      socket.emit("leave_room");
      socket.disconnect();
      socket.off();
      socket = null;
    }
    isConnected = false;
    isAuthenticated = false;
    isConnecting = false;
    isAuthenticating = false;
    currentListener = null;
  }

  /**
   * 发送玩家准备状态
   */
  public void sendPlayerReady(boolean ready) {
    if (isConnected && socket != null) {
      JsonObject readyData = new JsonObject();
      readyData.addProperty("ready", ready);
      Log.d(TAG, "发送消息: player_ready, 数据: " + readyData.toString());
      socket.emit("player_ready", readyData);
    }
  }

  /**
   * 发送投降请求
   */
  public void sendSurrender() {
    if (isConnected && socket != null) {
      JsonObject surrenderData = new JsonObject();
      Log.d(TAG, "发送消息: surrender");
      socket.emit("surrender", surrenderData);
    }
  }

  /**
   * 发送认证请求（带防抖）
   */
  private void sendAuthWithDebounce() {
    long currentTime = System.currentTimeMillis();

    // 如果刚连接成功，允许立即认证
    if (currentTime - lastAuthTime < DEBOUNCE_DELAY && lastAuthTime > 0) {
      Log.d(TAG, "认证请求被防抖拦截，距离上次认证: " + (currentTime - lastAuthTime) + "ms");
      return;
    }

    lastAuthTime = currentTime;
    isAuthenticating = true;

    // 从 AuthManager 获取当前用户的 token
    AuthManager authManager = AuthManager.getInstance(appContext);
    String userToken = authManager.getToken();

    if (userToken == null || userToken.isEmpty()) {
      Log.e(TAG, "无法获取用户 token，可能用户未登录");
      isAuthenticating = false;
      if (currentListener != null) {
        currentListener.onError("用户未登录，无法认证");
      }
      return;
    }

    JsonObject authData = new JsonObject();
    authData.addProperty("token", userToken);
    Log.d(TAG, "发送消息: auth, 数据: " + authData.toString());
    socket.emit("auth", authData);
  }

  /**
   * 请求房间状态
   */
  public void requestRoomState() {
    if (!isConnected || socket == null || !isAuthenticated) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastRoomStateRequestTime < DEBOUNCE_DELAY) {
      Log.d(TAG, "房间状态请求被防抖拦截");
      return;
    }

    lastRoomStateRequestTime = currentTime;
    Log.d(TAG, "发送消息: get_room_state");
    JsonObject emptyData = new JsonObject();
    socket.emit("get_room_state", emptyData);
  }

  /**
   * 发送通用 Socket 事件
   * 
   * @param eventName 事件名称
   * @param data      事件数据
   */
  public void emit(String eventName, JsonObject data) {
    if (isConnected && socket != null) {
      Log.d(TAG, "发送消息: " + eventName + ", 数据: " + (data != null ? data.toString() : "null"));
      if (data != null) {
        socket.emit(eventName, data);
      } else {
        socket.emit(eventName);
      }
    } else {
      Log.w(TAG, "Socket 未连接，无法发送事件: " + eventName);
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
    socket.on("room_not_found", onRoomNotFound);
    socket.on("room_closing", onRoomClosing);
    socket.on("room_closed", onRoomClosed);
    socket.on("player_joined", onPlayerJoined);
    socket.on("player_left", onPlayerLeft);

    // 游戏事件
    socket.on("player_ready_changed", onPlayerReadyChanged);
    socket.on("room_status", onRoomStatus);
    socket.on("game_state", onGameState);
    socket.on("game_started", onGameStarted);
    socket.on("player_surrendered", onPlayerSurrendered);
  }

  // 事件监听器实现
  private Emitter.Listener onConnect = args -> {
    Log.d(TAG, "接收事件: connect - Socket 连接成功");
    isConnected = true;
    isConnecting = false;
    isAuthenticated = false; // 重置认证状态
    hasShownInitialError = false; // 重置错误标志
    lastAuthTime = 0; // 重置认证防抖时间，允许立即认证
    if (currentListener != null) {
      currentListener.onConnectionChanged(true);
    }
  };

  private Emitter.Listener onDisconnect = args -> {
    Log.d(TAG, "接收事件: disconnect - 断开连接，原因: " + (args.length > 0 ? args[0].toString() : "未知"));
    isConnected = false;
    isConnecting = false;
    isAuthenticated = false;
    isAuthenticating = false;
    lastAuthTime = 0; // 重置防抖时间
    if (currentListener != null) {
      currentListener.onConnectionChanged(false);
    }
  };

  private Emitter.Listener onConnectError = args -> {
    String errorMsg = args.length > 0 ? args[0].toString() : "未知错误";
    Log.e(TAG, "接收事件: connect_error - 连接错误: " + errorMsg);

    // 只在首次连接失败时显示错误，避免重连时频繁弹出
    if (!hasShownInitialError && currentListener != null) {
      hasShownInitialError = true;
      // 只有在非网络切换等常见错误时才显示
      if (!errorMsg.contains("xhr poll error") && !errorMsg.contains("transport error")) {
        currentListener.onError("Socket连接失败: " + errorMsg);
      }
    }
    isConnecting = false;
  };

  private Emitter.Listener onNeedAuth = args -> {
    String authInfo = args.length > 0 ? args[0].toString() : "无认证信息";
    Log.d(TAG, "接收事件: need_auth - 需要认证: " + authInfo);
    sendAuthWithDebounce();
  };

  private Emitter.Listener onAuthSuccess = args -> {
    Log.d(TAG, "接收事件: auth_success");
    isAuthenticated = true;
    isAuthenticating = false;
    if (currentListener != null) {
      currentListener.onAuthSuccess();
    }
    // 自动请求房间状态
    requestRoomState();
  };

  private Emitter.Listener onAuthFail = args -> {
    Log.d(TAG, "接收事件: auth_fail, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    isAuthenticated = false;
    isAuthenticating = false;
    lastAuthTime = 0; // 认证失败时重置防抖，允许重新尝试
    String message = "认证失败";
    try {
      if (args.length > 0) {
        String responseStr = args[0].toString();
        // 如果响应是纯字符串，直接使用
        if (!responseStr.startsWith("{")) {
          message = responseStr;
        } else {
          JsonObject data = JsonParser.parseString(responseStr).getAsJsonObject();
          if (data.has("message")) {
            message = data.get("message").getAsString();
          }
        }
      }
    } catch (Exception e) {
      // 使用默认消息
      Log.w(TAG, "解析认证失败消息出错: " + e.getMessage());
    }

    if (currentListener != null) {
      currentListener.onAuthFail(message);
    }
  };

  private Emitter.Listener onRoomNotFound = args -> {
    Log.d(TAG, "接收事件: room_not_found, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    String message = "房间不存在";
    try {
      if (args.length > 0) {
        String responseStr = args[0].toString();
        // 如果响应是纯字符串，直接使用
        if (!responseStr.startsWith("{")) {
          message = responseStr;
        } else {
          JsonObject data = JsonParser.parseString(responseStr).getAsJsonObject();
          if (data.has("message")) {
            message = data.get("message").getAsString();
          }
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "解析房间不存在消息出错: " + e.getMessage());
    }

    if (currentListener != null) {
      currentListener.onRoomNotFound(message);
    }
  };

  private Emitter.Listener onRoomClosing = args -> {
    Log.d(TAG, "接收事件: room_closing, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onRoomClosing(data);
      }
    } catch (Exception e) {
      Log.e(TAG, "解析 room_closing 数据失败: " + e.getMessage());
    }
  };

  private Emitter.Listener onRoomClosed = args -> {
    Log.d(TAG, "接收事件: room_closed, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    String message = "房间已关闭";
    try {
      if (args.length > 0) {
        String responseStr = args[0].toString();
        // 如果响应是纯字符串，直接使用
        if (!responseStr.startsWith("{")) {
          message = responseStr;
        } else {
          JsonObject data = JsonParser.parseString(responseStr).getAsJsonObject();
          if (data.has("message")) {
            message = data.get("message").getAsString();
          }
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "解析房间关闭消息出错: " + e.getMessage());
    }

    if (currentListener != null) {
      currentListener.onRoomClosed(message);
    }
  };

  private Emitter.Listener onPlayerJoined = args -> {
    Log.d(TAG, "接收事件: player_joined, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onPlayerJoined(data);
      }
    } catch (Exception e) {
      Log.e(TAG, "解析 player_joined 数据失败: " + e.getMessage());
    }
  };

  private Emitter.Listener onPlayerLeft = args -> {
    Log.d(TAG, "接收事件: player_left, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onPlayerLeft(data);
      }
    } catch (Exception e) {
      Log.e(TAG, "解析 player_left 数据失败: " + e.getMessage());
    }
  };

  private Emitter.Listener onPlayerReadyChanged = args -> {
    Log.d(TAG, "接收事件: player_ready_changed, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onPlayerReadyChanged(data);
      }
    } catch (Exception e) {
      Log.e(TAG, "解析 player_ready_changed 数据失败: " + e.getMessage());
    }
  };

  private Emitter.Listener onRoomStatus = args -> {
    Log.d(TAG, "接收事件: room_status, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onRoomStatus(data);
      }
    } catch (Exception e) {
      Log.e(TAG, "解析 room_status 数据失败: " + e.getMessage());
    }
  };

  private Emitter.Listener onGameState = args -> {
    Log.d(TAG, "接收事件: game_state, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onGameState(data);
      }
    } catch (Exception e) {
      Log.e(TAG, "解析 game_state 数据失败: " + e.getMessage());
    }
  };

  private Emitter.Listener onGameStarted = args -> {
    Log.d(TAG, "接收事件: game_started, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onGameStarted(data);
      }
    } catch (Exception e) {
      if (currentListener != null) {
        currentListener.onError("游戏开始数据解析失败");
      }
    }
  };

  private Emitter.Listener onPlayerSurrendered = args -> {
    Log.d(TAG, "接收事件: player_surrendered, 数据: " + (args.length > 0 ? args[0].toString() : "无数据"));
    try {
      JsonObject data = JsonParser.parseString(args[0].toString()).getAsJsonObject();
      if (currentListener != null) {
        currentListener.onPlayerSurrendered(data);
      }
    } catch (Exception e) {
      Log.e(TAG, "解析 player_surrendered 数据失败: " + e.getMessage());
    }
  };
}
