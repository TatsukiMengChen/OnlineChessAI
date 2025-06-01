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

  private String roomId;
  private String roomName;
  private RoomApi roomApi;
  private Gson gson;

  // Socket.IO 相关
  private SocketManager socketManager;
  private boolean isReady = false;

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
    roomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);

    // 获取用户token
    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
    String userToken = prefs.getString("token", "");

    initViews();
    initSocketConnection(userToken);
  }

  private void initViews() {
    tvRoomName = findViewById(R.id.tv_room_name);
    tvRoomId = findViewById(R.id.tv_room_id);
    tvPlayer1Status = findViewById(R.id.tv_player1_status);
    tvPlayer2Status = findViewById(R.id.tv_player2_status);
    tvGameStatus = findViewById(R.id.tv_game_status);
    btnStartGame = findViewById(R.id.btn_start_game);
    btnQuitRoom = findViewById(R.id.btn_quit_room);

    tvRoomName.setText(roomName != null ? roomName : "");
    tvRoomId.setText("房间ID: " + (roomId != null ? roomId : ""));
    tvGameStatus.setText("连接中...");

    btnStartGame.setText("准备");
    btnStartGame.setEnabled(false);
    btnStartGame.setOnClickListener(v -> onReadyToggleClicked());
    btnQuitRoom.setOnClickListener(v -> onQuitRoomClicked());
  }

  private void initSocketConnection(String userToken) {
    socketManager = new SocketManager(roomId, userToken, this);
    socketManager.connect();
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
  public void onRoomStateUpdated(JsonObject roomState) {
    runOnUiThread(() -> updateRoomState(roomState));
  }

  @Override
  public void onUserEntered(String username) {
    runOnUiThread(() -> Toast.makeText(this, username + " 进入了房间", Toast.LENGTH_SHORT).show());
  }

  @Override
  public void onUserLeft(String username) {
    runOnUiThread(() -> Toast.makeText(this, username + " 离开了房间", Toast.LENGTH_SHORT).show());
  }

  @Override
  public void onPlayerReadyChanged(String username, boolean ready) {
    runOnUiThread(() -> Toast.makeText(this, username + (ready ? " 已准备" : " 取消准备"), Toast.LENGTH_SHORT).show());
  }

  @Override
  public void onGameStarted(JsonObject gameState) {
    runOnUiThread(() -> {
      tvGameStatus.setText("游戏开始！");
      btnStartGame.setEnabled(false);
      Toast.makeText(this, "游戏开始！", Toast.LENGTH_SHORT).show();

      // TODO: 跳转到游戏界面
      // GameActivity.start(this, roomId, gameState);
    });
  }

  @Override
  public void onError(String error) {
    runOnUiThread(() -> {
      tvGameStatus.setText("连接失败");
      Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    });
  }

  private void updateRoomState(JsonObject data) {
    try {
      JsonObject roomInfo = data.getAsJsonObject("roomInfo");
      String status = roomInfo.get("status").getAsString();

      // 更新房间状态显示
      switch (status) {
        case "waiting":
          tvGameStatus.setText("等待玩家");
          break;
        case "full":
          tvGameStatus.setText("房间已满");
          break;
        case "playing":
          tvGameStatus.setText("游戏进行中");
          btnStartGame.setEnabled(false);
          break;
        default:
          tvGameStatus.setText("房间状态: " + status);
      }

      // 更新玩家状态
      if (roomInfo.has("player1") && !roomInfo.get("player1").isJsonNull()) {
        JsonObject player1 = roomInfo.getAsJsonObject("player1");
        String player1Name = player1.get("username").getAsString();
        boolean player1Ready = player1.get("isReady").getAsBoolean();
        boolean player1Online = player1.get("isOnline").getAsBoolean();

        tvPlayer1Status.setText("玩家1: " + player1Name +
            (player1Ready ? " (已准备)" : " (未准备)") +
            (player1Online ? "" : " (离线)"));
        tvPlayer1Status.setVisibility(View.VISIBLE);
      } else {
        tvPlayer1Status.setText("玩家1: 等待加入");
        tvPlayer1Status.setVisibility(View.VISIBLE);
      }

      if (roomInfo.has("player2") && !roomInfo.get("player2").isJsonNull()) {
        JsonObject player2 = roomInfo.getAsJsonObject("player2");
        String player2Name = player2.get("username").getAsString();
        boolean player2Ready = player2.get("isReady").getAsBoolean();
        boolean player2Online = player2.get("isOnline").getAsBoolean();

        tvPlayer2Status.setText("玩家2: " + player2Name +
            (player2Ready ? " (已准备)" : " (未准备)") +
            (player2Online ? "" : " (离线)"));
        tvPlayer2Status.setVisibility(View.VISIBLE);
      } else {
        tvPlayer2Status.setText("玩家2: 等待加入");
        tvPlayer2Status.setVisibility(View.VISIBLE);
      }

    } catch (Exception e) {
      Toast.makeText(this, "房间状态更新失败", Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * 切换准备状态
   */
  private void onReadyToggleClicked() {
    if (!socketManager.isConnected()) {
      Toast.makeText(this, "连接断开，请重试", Toast.LENGTH_SHORT).show();
      return;
    }

    isReady = !isReady;
    socketManager.sendPlayerReady(isReady);

    btnStartGame.setText(isReady ? "取消准备" : "准备");
    btnStartGame.setEnabled(false);

    // 延迟重新启用按钮
    btnStartGame.postDelayed(() -> btnStartGame.setEnabled(true), 1000);
  }

  /**
   * 退出房间操作
   */
  private void onQuitRoomClicked() {
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
    new AlertDialog.Builder(this)
        .setTitle("退出房间")
        .setMessage("确定要退出房间并离开此页面吗？")
        .setPositiveButton("确定", (dialog, which) -> onQuitRoomClicked())
        .setNegativeButton("取消", null)
        .show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (socketManager != null) {
      socketManager.disconnect();
    }
  }
}
