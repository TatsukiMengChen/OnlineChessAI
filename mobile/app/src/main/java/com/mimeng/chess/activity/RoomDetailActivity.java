package com.mimeng.chess.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.mimeng.chess.R;
import com.mimeng.chess.api.room.RoomApi;
import com.mimeng.chess.api.ApiResponse;
import com.mimeng.chess.socket.SocketManager;
import com.mimeng.chess.socket.SocketEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

/**
 * 房间详情页，展示房间信息，预留开始游戏等操作
 */
public class RoomDetailActivity extends BaseActivity implements SocketEventListener {
  private static final String EXTRA_ROOM_ID = "room_id";
  private static final String EXTRA_ROOM_NAME = "room_name";

  private TextView tvRoomName;
  private TextView tvRoomId;
  private TextView tvPlayer1Status;
  private TextView tvPlayer2Status;
  private TextView tvGameStatus;
  private Button btnStartGame;
  private Button btnQuitRoom;
  private Button btnSurrender;

  private String roomId;
  private String roomName;
  private RoomApi roomApi;
  private Gson gson;

  // Socket.IO 相关
  private SocketManager socketManager;
  private boolean isReady = false;
  private boolean isGameStarted = false;

  // 防抖相关
  private long lastReadyToggleTime = 0;
  private static final long READY_DEBOUNCE_DELAY = 2000; // 2秒防抖，避免快速点击

  // 添加请求码常量
  private static final int REQUEST_CODE_GAME = 1001;

  public static void start(Context context, String roomId, String roomName) {
    Intent intent = new Intent(context, RoomDetailActivity.class);
    intent.putExtra(EXTRA_ROOM_ID, roomId);
    intent.putExtra(EXTRA_ROOM_NAME, roomName);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_room_detail);

    roomApi = new RoomApi();
    gson = new Gson();

    roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
    roomName = getIntent().getStringExtra(EXTRA_ROOM_NAME); // 获取用户token
    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
    String userToken = prefs.getString("token", "");

    initViews();
    initSocketConnection();
  }

  private void initViews() {
    tvRoomName = findViewById(R.id.tv_room_name);
    tvRoomId = findViewById(R.id.tv_room_id);
    tvPlayer1Status = findViewById(R.id.tv_player1_status);
    tvPlayer2Status = findViewById(R.id.tv_player2_status);
    tvGameStatus = findViewById(R.id.tv_game_status);
    btnStartGame = findViewById(R.id.btn_start_game);
    btnQuitRoom = findViewById(R.id.btn_quit_room);
    btnSurrender = findViewById(R.id.btn_surrender);

    tvRoomName.setText(roomName != null ? roomName : "");
    tvRoomId.setText("房间ID: " + (roomId != null ? roomId : ""));
    tvGameStatus.setText("连接中...");

    btnStartGame.setText("准备");
    btnStartGame.setEnabled(false);
    btnStartGame.setOnClickListener(v -> onReadyToggleClicked());
    btnQuitRoom.setOnClickListener(v -> onQuitRoomClicked());
    btnSurrender.setOnClickListener(v -> onSurrenderClicked());
  }

  private void initSocketConnection() {
    socketManager = SocketManager.getInstance(this);
    socketManager.registerListener(this, roomId);
  }

  // SocketEventListener 接口实现
  @Override
  public void onConnectionChanged(boolean connected) {
    runOnUiThread(() -> {
      if (connected) {
        tvGameStatus.setText("已连接，等待认证...");
      } else {
        tvGameStatus.setText("连接断开");
        btnStartGame.setEnabled(false);
      }
    });
  }

  @Override
  public void onAuthSuccess() {
    runOnUiThread(() -> {
      tvGameStatus.setText("认证成功，等待其他玩家...");
      btnStartGame.setEnabled(true);
    });
  }

  @Override
  public void onAuthFail(String message) {
    runOnUiThread(() -> {
      tvGameStatus.setText("认证失败");
      Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
      finish();
    });
  }

  @Override
  public void onRoomNotFound(String message) {
    runOnUiThread(() -> {
      tvGameStatus.setText("房间不存在");
      Toast.makeText(this, message, Toast.LENGTH_LONG).show();
      finish();
    });
  }

  @Override
  public void onPlayerJoined(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String userName = data.get("userName").getAsString();
        String message = data.has("message") ? data.get("message").getAsString() : userName + " 加入了房间";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        // 忽略解析错误
      }
    });
  }

  @Override
  public void onPlayerLeft(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String userName = data.get("userName").getAsString();
        String message = data.has("message") ? data.get("message").getAsString() : userName + " 离开了房间";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        // 忽略解析错误
      }
    });
  }

  @Override
  public void onPlayerReadyChanged(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String userName = data.get("userName").getAsString();
        boolean ready = data.get("ready").getAsBoolean();
        String message = data.has("message") ? data.get("message").getAsString()
            : userName + (ready ? " 已准备" : " 取消准备");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        // 忽略解析错误
      }
    });
  }

  @Override
  public void onRoomStatus(JsonObject data) {
    runOnUiThread(() -> updateRoomStatus(data));
  }

  @Override
  public void onGameState(JsonObject data) {
    runOnUiThread(() -> updateGameState(data));
  }

  @Override
  public void onGameStarted(JsonObject gameState) {
    runOnUiThread(() -> {
      Toast.makeText(this, "游戏开始！", Toast.LENGTH_SHORT).show();

      // 使用startActivityForResult启动游戏界面，以便接收游戏结束的结果
      Intent gameIntent = new Intent(this, GameActivity.class);
      gameIntent.putExtra("room_id", roomId);
      gameIntent.putExtra("game_state", gameState.toString());
      startActivityForResult(gameIntent, REQUEST_CODE_GAME);

      // 游戏开始后直接关闭房间详情页面
      finish();
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_CODE_GAME && resultCode == GameActivity.RESULT_GAME_ENDED) {
      // 游戏结束，设置结果通知调用者刷新列表
      setResult(RESULT_OK);
      finish();
    }
  }

  @Override
  public void onPlayerSurrendered(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String userName = data.get("userName").getAsString();
        Toast.makeText(this, userName + " 投降了，游戏结束", Toast.LENGTH_LONG).show();
      } catch (Exception e) {
        Toast.makeText(this, "游戏结束", Toast.LENGTH_LONG).show();
      }

      // 投降后直接退出Activity
      finish();
    });
  }

  @Override
  public void onRoomClosing(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String message = data.get("message").getAsString();
        int countdown = data.get("countdown").getAsInt();

        tvGameStatus.setText("房间即将关闭 (" + countdown + "秒)");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // 禁用所有操作按钮
        btnStartGame.setEnabled(false);
        btnQuitRoom.setEnabled(false);
        btnSurrender.setEnabled(false);

      } catch (Exception e) {
        // 使用默认消息
        tvGameStatus.setText("房间即将关闭");
        Toast.makeText(this, "游戏已结束，房间即将关闭", Toast.LENGTH_LONG).show();

        // 禁用所有操作按钮
        btnStartGame.setEnabled(false);
        btnQuitRoom.setEnabled(false);
        btnSurrender.setEnabled(false);
      }
    });
  }

  @Override
  public void onRoomClosed(String message) {
    runOnUiThread(() -> {
      Toast.makeText(this, message, Toast.LENGTH_LONG).show();
      // 房间被关闭，直接退出Activity
      finish();
    });
  }

  @Override
  public void onError(String error) {
    runOnUiThread(() -> {
      // 只显示重要错误，避免频繁弹出网络切换等常见错误
      if (!error.contains("xhr poll error") && !error.contains("transport error")) {
        tvGameStatus.setText("连接失败");
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onPlayerColorAssigned(JsonObject data) {
    // TODO: 这里可以根据需要处理分配颜色后的逻辑，比如弹窗提示或UI更新
    // 示例：Toast.makeText(this, "你被分配为" + data.get("color").getAsString(),
    // Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onGameEnded(JsonObject data) {
    runOnUiThread(() -> {
      try {
        String result = data.has("result") ? data.get("result").getAsString() : "游戏结束";
        String reason = data.has("reason") ? data.get("reason").getAsString() : "";

        String message = result;
        if (!reason.isEmpty()) {
          message += " (" + reason + ")";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // 重置游戏状态，重新显示房间界面
        isGameStarted = false;
        btnStartGame.setVisibility(View.VISIBLE);
        btnQuitRoom.setVisibility(View.VISIBLE);
        btnSurrender.setVisibility(View.GONE);
        btnStartGame.setText("准备");
        tvGameStatus.setText("游戏已结束");
      } catch (Exception e) {
        Toast.makeText(this, "游戏结束", Toast.LENGTH_LONG).show();
        isGameStarted = false;
        btnStartGame.setVisibility(View.VISIBLE);
        btnQuitRoom.setVisibility(View.VISIBLE);
        btnSurrender.setVisibility(View.GONE);
      }
    });
  }

  private void updateRoomStatus(JsonObject data) {
    try {
      String status = data.get("status").getAsString();
      String message = data.has("message") ? data.get("message").getAsString() : "";

      // 更新房间状态显示
      switch (status) {
        case "WAITING":
          tvGameStatus.setText("等待玩家准备");
          // 确保显示正确的按钮
          btnStartGame.setVisibility(View.VISIBLE);
          btnQuitRoom.setVisibility(View.VISIBLE);
          btnSurrender.setVisibility(View.GONE);
          break;
        case "PLAYING":
          tvGameStatus.setText("游戏进行中");
          // 游戏开始，隐藏所有按钮，等待跳转
          btnStartGame.setVisibility(View.GONE);
          btnQuitRoom.setVisibility(View.GONE);
          btnSurrender.setVisibility(View.GONE);
          break;
        default:
          tvGameStatus.setText(message.isEmpty() ? "房间状态: " + status : message);
      }

      // 更新玩家状态
      if (data.has("players")) {
        JsonObject players = data.getAsJsonObject("players");

        if (players.has("red") && !players.get("red").isJsonNull()) {
          JsonObject redPlayer = players.getAsJsonObject("red");
          String name = redPlayer.get("name").getAsString();
          boolean ready = redPlayer.get("ready").getAsBoolean();
          boolean online = redPlayer.get("online").getAsBoolean();

          tvPlayer1Status.setText("红方: " + name +
              (ready ? " (已准备)" : " (未准备)") +
              (online ? "" : " (离线)"));
        } else {
          tvPlayer1Status.setText("红方: 等待加入");
        }

        if (players.has("black") && !players.get("black").isJsonNull()) {
          JsonObject blackPlayer = players.getAsJsonObject("black");
          String name = blackPlayer.get("name").getAsString();
          boolean ready = blackPlayer.get("ready").getAsBoolean();
          boolean online = blackPlayer.get("online").getAsBoolean();

          tvPlayer2Status.setText("黑方: " + name +
              (ready ? " (已准备)" : " (未准备)") +
              (online ? "" : " (离线)"));
        } else {
          tvPlayer2Status.setText("黑方: 等待加入");
        }
      }

    } catch (Exception e) {
      Toast.makeText(this, "房间状态更新失败", Toast.LENGTH_SHORT).show();
    }
  }

  private void updateGameState(JsonObject data) {
    try {
      String status = data.get("status").getAsString();

      switch (status) {
        case "PLAYING":
          String currentPlayer = data.get("currentPlayer").getAsString();
          tvGameStatus.setText("游戏进行中 - 当前: " + ("RED".equals(currentPlayer) ? "红方" : "黑方"));
          btnStartGame.setEnabled(false);
          break;
        default:
          tvGameStatus.setText("游戏状态: " + status);
      }

      // 更新玩家信息
      if (data.has("redPlayer")) {
        JsonObject redPlayer = data.getAsJsonObject("redPlayer");
        String name = redPlayer.get("name").getAsString();
        boolean ready = redPlayer.get("ready").getAsBoolean();
        tvPlayer1Status.setText("红方: " + name + (ready ? " (已准备)" : " (未准备)"));
      }

      if (data.has("blackPlayer")) {
        JsonObject blackPlayer = data.getAsJsonObject("blackPlayer");
        String name = blackPlayer.get("name").getAsString();
        boolean ready = blackPlayer.get("ready").getAsBoolean();
        tvPlayer2Status.setText("黑方: " + name + (ready ? " (已准备)" : " (未准备)"));
      }

    } catch (Exception e) {
      // 忽略解析错误
    }
  }

  /**
   * 切换准备状态（优化防抖）
   */
  private void onReadyToggleClicked() {
    if (!socketManager.isConnected()) {
      Toast.makeText(this, "连接断开，请重试", Toast.LENGTH_SHORT).show();
      return;
    }

    // 防抖检查
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastReadyToggleTime < READY_DEBOUNCE_DELAY) {
      Toast.makeText(this, "操作过于频繁，请稍后再试", Toast.LENGTH_SHORT).show();
      return;
    }
    lastReadyToggleTime = currentTime;

    isReady = !isReady;
    socketManager.sendPlayerReady(isReady);

    btnStartGame.setText(isReady ? "取消准备" : "准备");
    btnStartGame.setEnabled(false);

    // 延迟重新启用按钮
    btnStartGame.postDelayed(() -> btnStartGame.setEnabled(true), READY_DEBOUNCE_DELAY);
  }

  /**
   * 投降操作 - 已移动到GameActivity
   */
  private void onSurrenderClicked() {
    Toast.makeText(this, "请在游戏界面进行投降操作", Toast.LENGTH_SHORT).show();
  }

  /**
   * 退出房间操作
   */
  private void onQuitRoomClicked() {
    // 如果游戏已开始，不能直接退出
    if (isGameStarted) {
      Toast.makeText(this, "游戏进行中，请先投降再退出", Toast.LENGTH_SHORT).show();
      return;
    }

    // 先断开Socket连接
    if (socketManager != null) {
      socketManager.disconnect();
    }

    // 然后调用HTTP API退出房间
    if (roomId == null)
      return;
    roomApi.quitRoom(roomId, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> Toast.makeText(RoomDetailActivity.this, "网络错误，退出失败", Toast.LENGTH_SHORT).show());
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String responseBody = response.body().string();
        runOnUiThread(() -> {
          if (response.isSuccessful()) {
            try {
              ApiResponse<?> apiResponse = gson.fromJson(responseBody, ApiResponse.class);
              if (apiResponse.code == 200) {
                Toast.makeText(RoomDetailActivity.this, "已退出房间", Toast.LENGTH_SHORT).show();
                finish();
              } else {
                Toast.makeText(RoomDetailActivity.this, apiResponse.msg != null ? apiResponse.msg : "退出房间失败",
                    Toast.LENGTH_SHORT).show();
              }
            } catch (Exception e) {
              Toast.makeText(RoomDetailActivity.this, "响应解析失败", Toast.LENGTH_SHORT).show();
            }
          } else {
            Toast.makeText(RoomDetailActivity.this, "退出房间失败", Toast.LENGTH_SHORT).show();
          }
        });
      }
    });
  }

  @Override
  public void onBackPressed() {
    if (isGameStarted) {
      Toast.makeText(this, "游戏进行中，请先投降再退出", Toast.LENGTH_SHORT).show();
      return;
    }

    new AlertDialog.Builder(this)
        .setTitle("退出房间")
        .setMessage("确定要退出房间并离开此页面吗？")
        .setPositiveButton("确定", (dialog, which) -> onQuitRoomClicked())
        .setNegativeButton("取消", null)
        .show();

    super.onBackPressed();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (socketManager != null) {
      socketManager.unregisterListener(this);
    }
  }
}
