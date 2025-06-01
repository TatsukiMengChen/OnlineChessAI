package com.mimeng.chess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mimeng.chess.R;
import com.mimeng.chess.api.auth.LoginRes;
import com.mimeng.chess.api.room.CreateRoomReq;
import com.mimeng.chess.api.room.RoomApi;
import com.mimeng.chess.utils.AuthManager;

import okhttp3.Call;
import okhttp3.Response;
import java.io.IOException;

/**
 * 主页面 - 登录后的主界面
 */
public class MainActivity extends AppCompatActivity {

  private TextView tvWelcome;
  private TextView tvUserInfo;
  private Button btnAiGame;
  private Button btnOnlineGame;
  private Button btnTestApi;
  private Button btnLogout;
  private AuthManager authManager;
  private long lastBackPressedTime = 0;
  private static final int BACK_PRESS_INTERVAL = 2000; // 2秒内双击退出

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // 删除标题栏
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // 设置状态栏图标为深色，适配白色背景
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

    initViews();
    initData();
    setupListeners();
    loadUserInfo();
  }

  /**
   * 初始化视图
   */
  private void initViews() {
    tvWelcome = findViewById(R.id.tv_welcome);
    tvUserInfo = findViewById(R.id.tv_user_info);
    btnAiGame = findViewById(R.id.btn_ai_game);
    btnOnlineGame = findViewById(R.id.btn_online_game);
    btnTestApi = findViewById(R.id.btn_test_api);
    btnLogout = findViewById(R.id.btn_logout);
  }

  /**
   * 初始化数据
   */
  private void initData() {
    authManager = AuthManager.getInstance(this);
  }

  /**
   * 设置监听器
   */
  private void setupListeners() {
    btnAiGame.setOnClickListener(v -> startAiGame());
    btnOnlineGame.setOnClickListener(v -> startOnlineGame());
    btnTestApi.setOnClickListener(v -> testApiWithAuth());
    btnLogout.setOnClickListener(v -> showLogoutDialog());
  }

  /**
   * 加载用户信息
   */
  private void loadUserInfo() {
    LoginRes.User user = authManager.getUser();
    if (user != null) {
      updateUserDisplay(user);
    } else {
      // 如果没有用户信息，跳转到登录页
      navigateToLogin();
    }
  }

  /**
   * 更新用户显示信息
   */
  private void updateUserDisplay(LoginRes.User user) {
    tvWelcome.setText("欢迎回来！");
    String userInfo = String.format("邮箱: %s\nID: %d", user.email, user.id);
    tvUserInfo.setText(userInfo);
  }

  /**
   * 开始人机对战
   */
  private void startAiGame() {
    // TODO: 实现人机对战功能
    showMessage("人机对战功能即将上线");
  }

  /**
   * 开始在线对战
   */
  private void startOnlineGame() {
    // TODO: 实现在线对战功能
    showMessage("在线对战功能即将上线");
  }

  /**
   * 测试需要认证的API
   */
  private void testApiWithAuth() {
    // 测试获取房间列表 - 这个接口不需要认证
    RoomApi.instance.listRooms(new okhttp3.Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> showMessage("房间列表请求失败: " + e.getMessage()));
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String responseBody = response.body().string();
        runOnUiThread(() -> {
          if (response.isSuccessful()) {
            showMessage("房间列表获取成功");
          } else {
            showMessage("房间列表请求失败: " + response.code());
          }
        });
      }
    });

    // 测试创建房间 - 这个接口需要认证
    CreateRoomReq createReq = new CreateRoomReq("测试房间");
    RoomApi.instance.createRoom(createReq, new okhttp3.Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        runOnUiThread(() -> showMessage("创建房间请求失败: " + e.getMessage()));
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String responseBody = response.body().string();
        runOnUiThread(() -> {
          if (response.isSuccessful()) {
            showMessage("创建房间成功！拦截器已自动添加Bearer token");
          } else {
            showMessage("创建房间失败: " + response.code() + " - " + responseBody);
          }
        });
      }
    });
  }

  /**
   * 显示退出登录确认对话框
   */
  private void showLogoutDialog() {
    new AlertDialog.Builder(this)
        .setTitle("退出登录")
        .setMessage("确定要退出登录吗？")
        .setPositiveButton("确定", (dialog, which) -> performLogout())
        .setNegativeButton("取消", null)
        .show();
  }

  /**
   * 执行退出登录
   */
  private void performLogout() {
    authManager.logout();
    showMessage("已退出登录");
    navigateToLogin();
  }

  /**
   * 跳转到登录页
   */
  private void navigateToLogin() {
    Intent intent = new Intent(this, LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  /**
   * 显示消息
   */
  private void showMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onBackPressed() {
    // 双击退出应用
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastBackPressedTime > BACK_PRESS_INTERVAL) {
      lastBackPressedTime = currentTime;
      showMessage("再按一次退出应用");
    } else {
      super.onBackPressed();
    }
  }
}
