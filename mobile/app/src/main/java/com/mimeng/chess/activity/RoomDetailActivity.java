package com.mimeng.chess.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.mimeng.chess.R;
import com.mimeng.chess.api.room.Room;
import com.mimeng.chess.api.room.RoomApi;
import com.mimeng.chess.api.ApiResponse;
import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

/**
 * 房间详情页，展示房间信息，预留开始游戏等操作
 */
public class RoomDetailActivity extends BaseActivity {
  private static final String EXTRA_ROOM_ID = "room_id";
  private static final String EXTRA_ROOM_NAME = "room_name";

  private TextView tvRoomName;
  private TextView tvRoomId;
  private Button btnStartGame;
  private Button btnQuitRoom;

  private String roomId;
  private String roomName;
  private RoomApi roomApi;
  private Gson gson;

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

    tvRoomName = findViewById(R.id.tv_room_name);
    tvRoomId = findViewById(R.id.tv_room_id);
    btnStartGame = findViewById(R.id.btn_start_game);
    btnQuitRoom = findViewById(R.id.btn_quit_room);

    tvRoomName.setText(roomName != null ? roomName : "");
    tvRoomId.setText("房间ID: " + (roomId != null ? roomId : ""));

    btnStartGame.setOnClickListener(v -> onStartGameClicked());
    btnQuitRoom.setOnClickListener(v -> onQuitRoomClicked());
  }

  /**
   * 预留：开始游戏逻辑（仅UI和方法占位，不实现Socket）
   */
  private void onStartGameClicked() {
    // TODO: 预留Socket.IO开始游戏逻辑
    Toast.makeText(this, "开始游戏功能待实现", Toast.LENGTH_SHORT).show();
  }

  /**
   * 退出房间操作
   */
  private void onQuitRoomClicked() {
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
}
