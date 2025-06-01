package com.mimeng.chess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.mimeng.chess.R;
import com.mimeng.chess.api.auth.LoginRes;
import com.mimeng.chess.dialog.ChangePasswordDialog;
import com.mimeng.chess.utils.AuthManager;

/**
 * 主页面 - 登录后的主界面
 */
public class MainActivity extends BaseActivity {
  private TextView tvWelcome;
  private TextView tvUserInfo;
  private Button btnAiGame;
  private Button btnOnlineGame;
  private Button btnChangePassword;
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

    // 应用仅状态栏的内边距策略
    applyStatusBarOnlyInsets();

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
    btnChangePassword = findViewById(R.id.btn_change_password);
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
    btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
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
    Intent intent = new Intent(this, RoomListActivity.class);
    startActivity(intent);
  }

  /**
   * 显示修改密码对话框
   */
  private void showChangePasswordDialog() {
    ChangePasswordDialog dialog = new ChangePasswordDialog(this);
    dialog.setOnPasswordChangedListener(() -> {
      showMessage("密码修改成功，下次登录时请使用新密码");
    });
    dialog.show();
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
