package com.mimeng.chess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.mimeng.chess.BuildConfig;
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
  private Button btnOnlineGame;
  private Button btnChangePassword;
  private Button btnLogout;
  private Button btnAbout;
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
    btnOnlineGame = findViewById(R.id.btn_online_game);
    btnChangePassword = findViewById(R.id.btn_change_password);
    btnLogout = findViewById(R.id.btn_logout);
    btnAbout = findViewById(R.id.btn_about);
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
    btnOnlineGame.setOnClickListener(v -> startOnlineGame());
    btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    btnLogout.setOnClickListener(v -> showLogoutDialog());
    btnAbout.setOnClickListener(v -> showAboutDialog());
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
   * 显示关于对话框
   */
  private void showAboutDialog() {
    // 获取基本应用信息
    String appName = getAppInfo("APP_NAME", "OnlineChessAI");
    String version = getAppInfo("APP_VERSION", "1.0.0");
    String completionDate = getAppInfo("COMPLETION_DATE", "2025年6月2日");
    String projectDescription = getAppInfo("PROJECT_DESCRIPTION", "基于人工智能的在线象棋对战平台");
    // 获取作者信息
    String authorName = getAppInfo("AUTHOR_NAME", "孟晨");
    String studentId = getAppInfo("STUDENT_ID", "2021XXXXXX");
    String className = getAppInfo("CLASS_NAME", "计算机科学与技术2021级X班");
    String university = getAppInfo("UNIVERSITY", "某某大学");
    String department = getAppInfo("DEPARTMENT", "计算机学院");

    // 获取技术和版权信息
    String technologies = getAppInfo("TECHNOLOGIES", "Android, Java, Socket.IO, AI算法");
    String copyright = getAppInfo("COPYRIGHT", "© 2025 孟晨. 保留所有权利。");
    String projectType = getAppInfo("PROJECT_TYPE", "毕业设计项目");

    // 获取致谢信息
    String acknowledgments = getAppInfo("ACKNOWLEDGMENTS", "感谢指导老师的悉心指导，感谢同学们的帮助与支持。");
    String specialThanks = getAppInfo("SPECIAL_THANKS", "特别感谢开源社区提供的优秀框架和工具。");

    String aboutMessage = String.format(
        "📱 %s\n" +
            "版本 %s\n\n" +
            "💡 项目简介\n" +
            "%s\n\n" + "👨‍🎓 开发者信息\n" +
            "姓名：%s\n" +
            "学号：%s\n" +
            "班级：%s\n" +
            "学校：%s\n" +
            "院系：%s\n\n" +
            "🛠️ 技术栈\n" +
            "%s\n\n" +
            "📅 项目信息\n" +
            "类型：%s\n" +
            "完成时间：%s\n\n" +
            "📄 版权声明\n" +
            "%s\n\n" +
            "🙏 致谢\n" +
            "%s\n\n" +
            "%s",
        appName, version, projectDescription,
        authorName, studentId, className, university, department,
        technologies,
        projectType, completionDate,
        copyright,
        acknowledgments, specialThanks);

    new AlertDialog.Builder(this)
        .setTitle("关于 " + appName)
        .setMessage(aboutMessage)
        .setPositiveButton("确定", null)
        .setIcon(R.drawable.ic_info_24)
        .show();
  }

  /**
   * 获取应用信息
   */
  private String getAppInfo(String key, String defaultValue) {
    try {
      // 使用反射获取 BuildConfig 中的字段
      java.lang.reflect.Field field = BuildConfig.class.getField(key);
      Object value = field.get(null);
      return value != null ? value.toString() : defaultValue;
    } catch (Exception e) {
      return defaultValue;
    }
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
