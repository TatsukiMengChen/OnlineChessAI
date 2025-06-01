package com.mimeng.chess.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.mimeng.chess.R;
import com.mimeng.chess.adapter.RoomListAdapter;
import com.mimeng.chess.api.room.Room;
import com.mimeng.chess.api.room.RoomApi;
import com.mimeng.chess.api.room.RoomListRes;
import com.mimeng.chess.api.ApiResponse;
import com.mimeng.chess.dialog.CreateRoomDialog;
import com.mimeng.chess.utils.AuthManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

/**
 * 房间列表页面
 */
public class RoomListActivity extends BaseActivity implements RoomListAdapter.OnRoomActionListener {
  private ImageView ivBack;
  private Button btnRefresh;
  private Button btnCreateRoom;
  private RecyclerView rvRooms;
  private LinearLayout layoutEmpty;
  private LinearLayout layoutLoading;

  private RoomListAdapter adapter;
  private RoomApi roomApi;
  private AuthManager authManager;
  private Gson gson;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState); // 删除标题栏
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    setContentView(R.layout.activity_room_list);

    initData();
    initViews();
    setupListeners();
    setupWindowInsets();
    loadRoomList();
  }

  private void initData() {
    roomApi = new RoomApi();
    authManager = AuthManager.getInstance(this);
    gson = new Gson();
    adapter = new RoomListAdapter(authManager);
    adapter.setOnRoomActionListener(this);
  }

  private void initViews() {
    ivBack = findViewById(R.id.iv_back);
    btnRefresh = findViewById(R.id.btn_refresh);
    btnCreateRoom = findViewById(R.id.btn_create_room);
    rvRooms = findViewById(R.id.rv_rooms);
    layoutEmpty = findViewById(R.id.layout_empty);
    layoutLoading = findViewById(R.id.layout_loading);

    // 设置RecyclerView
    rvRooms.setLayoutManager(new LinearLayoutManager(this));
    rvRooms.setAdapter(adapter);
  }

  private void setupListeners() {
    ivBack.setOnClickListener(v -> finish());
    btnRefresh.setOnClickListener(v -> loadRoomList());
    btnCreateRoom.setOnClickListener(v -> showCreateRoomDialog());
  }

  /**
   * 设置窗口内边距适配
   */
  private void setupWindowInsets() {
    // 为工具栏应用状态栏内边距
    View toolbar = findViewById(R.id.toolbar);
    if (toolbar != null) {
      applyStatusBarInsets(toolbar);
    }
  }

  /**
   * 加载房间列表
   */
  private void loadRoomList() {
    showLoading(true);

    roomApi.listRooms(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> {
          showLoading(false);
          showMessage("网络错误，请检查网络连接");
          showEmptyState(true);
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String responseBody = response.body().string();

        runOnUiThread(() -> {
          showLoading(false);

          if (response.isSuccessful()) {
            try {
              RoomListRes roomListRes = gson.fromJson(responseBody, RoomListRes.class);

              if (roomListRes.code == 200) {
                List<Room> rooms = roomListRes.data;
                adapter.updateRooms(rooms);

                if (rooms == null || rooms.isEmpty()) {
                  showEmptyState(true);
                } else {
                  showEmptyState(false);
                }
              } else {
                showMessage(roomListRes.msg != null ? roomListRes.msg : "获取房间列表失败");
                showEmptyState(true);
              }
            } catch (Exception e) {
              e.printStackTrace();
              showMessage("数据解析失败");
              showEmptyState(true);
            }
          } else {
            try {
              ApiResponse<?> errorResponse = gson.fromJson(responseBody, ApiResponse.class);
              showMessage(errorResponse.msg != null ? errorResponse.msg : "获取房间列表失败");
            } catch (Exception e) {
              showMessage("获取房间列表失败");
            }
            showEmptyState(true);
          }
        });
      }
    });
  }

  /**
   * 显示创建房间对话框
   */
  private void showCreateRoomDialog() {
    CreateRoomDialog dialog = new CreateRoomDialog(this);
    dialog.setOnRoomCreatedListener(room -> {
      // 房间创建成功，刷新列表
      loadRoomList();
    });
    dialog.show();
  }

  /**
   * 显示加载状态
   */
  private void showLoading(boolean show) {
    layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    rvRooms.setVisibility(show ? View.GONE : View.VISIBLE);
    if (show) {
      layoutEmpty.setVisibility(View.GONE);
    }
  }

  /**
   * 显示空状态
   */
  private void showEmptyState(boolean show) {
    layoutEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
    rvRooms.setVisibility(show ? View.GONE : View.VISIBLE);
  }

  /**
   * 显示消息
   */
  private void showMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  // RoomListAdapter.OnRoomActionListener 实现

  @Override
  public void onJoinRoom(Room room) {
    new AlertDialog.Builder(this)
        .setTitle("加入房间")
        .setMessage("确定要加入房间 \"" + room.name + "\" 吗？")
        .setPositiveButton("确定", (dialog, which) -> performJoinRoom(room))
        .setNegativeButton("取消", null)
        .show();
  }

  @Override
  public void onQuitRoom(Room room) {
    new AlertDialog.Builder(this)
        .setTitle("退出房间")
        .setMessage("确定要退出房间 \"" + room.name + "\" 吗？")
        .setPositiveButton("确定", (dialog, which) -> performQuitRoom(room))
        .setNegativeButton("取消", null)
        .show();
  }

  @Override
  public void onCloseRoom(Room room) {
    new AlertDialog.Builder(this)
        .setTitle("关闭房间")
        .setMessage("确定要关闭房间 \"" + room.name + "\" 吗？\n关闭后房间将无法再使用。")
        .setPositiveButton("确定", (dialog, which) -> performCloseRoom(room))
        .setNegativeButton("取消", null)
        .show();
  }

  /**
   * 执行加入房间操作
   */
  private void performJoinRoom(Room room) {
    roomApi.joinRoom(room.id, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> showMessage("网络错误，请检查网络连接"));
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String responseBody = response.body().string();

        runOnUiThread(() -> {
          if (response.isSuccessful()) {
            try {
              ApiResponse<Room> apiResponse = gson.fromJson(responseBody,
                  new com.google.gson.reflect.TypeToken<ApiResponse<Room>>() {
                  }.getType());

              if (apiResponse.code == 200) {
                showMessage("成功加入房间");
                loadRoomList(); // 刷新列表
              } else {
                showMessage(apiResponse.msg != null ? apiResponse.msg : "加入房间失败");
              }
            } catch (Exception e) {
              e.printStackTrace();
              showMessage("响应解析失败");
            }
          } else {
            try {
              ApiResponse<?> errorResponse = gson.fromJson(responseBody, ApiResponse.class);
              showMessage(errorResponse.msg != null ? errorResponse.msg : "加入房间失败");
            } catch (Exception e) {
              showMessage("加入房间失败");
            }
          }
        });
      }
    });
  }

  /**
   * 执行退出房间操作
   */
  private void performQuitRoom(Room room) {
    roomApi.quitRoom(room.id, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> showMessage("网络错误，请检查网络连接"));
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String responseBody = response.body().string();

        runOnUiThread(() -> {
          if (response.isSuccessful()) {
            try {
              ApiResponse<?> apiResponse = gson.fromJson(responseBody, ApiResponse.class);

              if (apiResponse.code == 200) {
                showMessage("已退出房间");
                loadRoomList(); // 刷新列表
              } else {
                showMessage(apiResponse.msg != null ? apiResponse.msg : "退出房间失败");
              }
            } catch (Exception e) {
              e.printStackTrace();
              showMessage("响应解析失败");
            }
          } else {
            try {
              ApiResponse<?> errorResponse = gson.fromJson(responseBody, ApiResponse.class);
              showMessage(errorResponse.msg != null ? errorResponse.msg : "退出房间失败");
            } catch (Exception e) {
              showMessage("退出房间失败");
            }
          }
        });
      }
    });
  }

  /**
   * 执行关闭房间操作
   */
  private void performCloseRoom(Room room) {
    roomApi.closeRoom(room.id, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> showMessage("网络错误，请检查网络连接"));
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String responseBody = response.body().string();

        runOnUiThread(() -> {
          if (response.isSuccessful()) {
            try {
              ApiResponse<?> apiResponse = gson.fromJson(responseBody, ApiResponse.class);

              if (apiResponse.code == 200) {
                showMessage("房间已关闭");
                loadRoomList(); // 刷新列表
              } else {
                showMessage(apiResponse.msg != null ? apiResponse.msg : "关闭房间失败");
              }
            } catch (Exception e) {
              e.printStackTrace();
              showMessage("响应解析失败");
            }
          } else {
            try {
              ApiResponse<?> errorResponse = gson.fromJson(responseBody, ApiResponse.class);
              showMessage(errorResponse.msg != null ? errorResponse.msg : "关闭房间失败");
            } catch (Exception e) {
              showMessage("关闭房间失败");
            }
          }
        });
      }
    });
  }
}
