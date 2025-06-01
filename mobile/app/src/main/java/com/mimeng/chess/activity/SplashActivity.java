package com.mimeng.chess.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mimeng.chess.R;
import com.mimeng.chess.utils.AuthManager;

/**
 * 启动页面
 * 检测用户登录状态，自动跳转到相应页面
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

  private static final int SPLASH_DELAY = 2000; // 2秒延迟

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // 隐藏状态栏和导航栏，实现全屏效果
    getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    // 删除标题栏
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    // 延迟检查登录状态并跳转
    new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginAndNavigate, SPLASH_DELAY);
  }

  /**
   * 检查登录状态并导航到相应页面
   */
  private void checkLoginAndNavigate() {
    AuthManager authManager = AuthManager.getInstance(this);

    Intent intent;
    if (authManager.isLoggedIn()) {
      // 已登录，跳转到主页面
      intent = new Intent(this, MainActivity.class);
    } else {
      // 未登录，跳转到登录页面
      intent = new Intent(this, LoginActivity.class);
    }

    startActivity(intent);
    finish(); // 结束启动页，防止用户按返回键回到启动页
  }
}
