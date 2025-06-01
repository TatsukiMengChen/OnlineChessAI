package com.mimeng.chess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.mimeng.chess.R;
import com.mimeng.chess.api.auth.AuthApi;
import com.mimeng.chess.api.auth.LoginReq;
import com.mimeng.chess.api.auth.LoginRes;
import com.mimeng.chess.utils.AuthManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

/**
 * 登录页面
 */
public class LoginActivity extends AppCompatActivity {
  private EditText etEmail;
  private EditText etPassword;
  private Button btnLogin;
  private TextView tvRegister;
  private TextView tvForgotPassword;
  private ScrollView scrollView;

  private AuthManager authManager;
  private Gson gson;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // 删除标题栏
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    // 启用沉浸式状态栏，让内容延伸到状态栏下方
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    setContentView(R.layout.activity_login);
    initViews();
    setupStatusBarPadding();
    initData();
    setupListeners();
  }

  /**
   * 设置状态栏填充
   */
  private void setupStatusBarPadding() {
    View statusBarSpacer = findViewById(R.id.status_bar_spacer);
    if (statusBarSpacer != null) {
      ViewCompat.setOnApplyWindowInsetsListener(statusBarSpacer, (v, insets) -> {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
        v.setPadding(0, systemBars.top, 0, 0);
        return insets;
      });
    }
  }

  /**
   * 初始化视图
   */
  private void initViews() {
    etEmail = findViewById(R.id.et_email);
    etPassword = findViewById(R.id.et_password);
    btnLogin = findViewById(R.id.btn_login);
    tvRegister = findViewById(R.id.tv_register);
    tvForgotPassword = findViewById(R.id.tv_forgot_password);
    scrollView = findViewById(R.id.scroll_view);
  }

  /**
   * 初始化数据
   */
  private void initData() {
    authManager = AuthManager.getInstance(this);
    gson = new Gson();
  }

  /**
   * 设置监听器
   */
  private void setupListeners() {
    btnLogin.setOnClickListener(v -> performLogin());
    tvRegister.setOnClickListener(v -> navigateToRegister());
    tvForgotPassword.setOnClickListener(v -> navigateToForgotPassword());

    // 设置输入框的IME监听器
    setupInputListeners();
  }

  /**
   * 设置输入框的软键盘监听器
   */
  private void setupInputListeners() {
    // 邮箱输入框按下"下一步"时焦点移到密码输入框
    etEmail.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_NEXT) {
        etPassword.requestFocus();
        return true;
      }
      return false;
    });

    // 密码输入框按下"完成"时执行登录
    etPassword.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        hideKeyboard();
        performLogin();
        return true;
      }
      return false;
    });

    // 输入框获得焦点时滚动到可见位置（竖屏优化）
    etEmail.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        // 延迟滚动，等待软键盘弹出
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });

    etPassword.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        // 延迟滚动，等待软键盘弹出
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });
  }

  /**
   * 滚动到指定视图位置
   */
  private void scrollToView(View view) {
    scrollView.post(() -> {
      // 简化滚动逻辑，专为竖屏优化
      int[] location = new int[2];
      view.getLocationInWindow(location);
      int viewTop = location[1];

      // 获取屏幕高度和键盘高度的估算值
      int screenHeight = getResources().getDisplayMetrics().heightPixels;
      int keyboardHeight = screenHeight / 3; // 估算键盘高度为屏幕高度的1/3
      int visibleHeight = screenHeight - keyboardHeight;

      // 如果输入框被键盘遮挡，则滚动到合适位置
      if (viewTop > visibleHeight - view.getHeight() - 100) {
        int scrollY = viewTop - visibleHeight + view.getHeight() + 100;
        scrollView.smoothScrollTo(0, Math.max(0, scrollY));
      }
    });
  }

  /**
   * 隐藏软键盘
   */
  private void hideKeyboard() {
    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    View currentFocus = getCurrentFocus();
    if (currentFocus != null && imm != null) {
      imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
    }
  }

  /**
   * 执行登录
   */
  private void performLogin() {
    // 隐藏软键盘
    hideKeyboard();

    String email = etEmail.getText().toString().trim();
    String password = etPassword.getText().toString().trim();

    // 输入验证
    if (!validateInput(email, password)) {
      return;
    }

    // 设置加载状态
    setLoadingState(true);

    // 调用登录API
    LoginReq req = new LoginReq(email, password);
    AuthApi.instance.login(req, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e("LOGIN", "登录请求失败: " + e.getMessage(), e);
        runOnUiThread(() -> {
          setLoadingState(false);
          showMessage("网络错误，请稍后重试");
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String body = response.body() != null ? response.body().string() : "";
        Log.d("LOGIN", "登录响应: " + body);

        runOnUiThread(() -> {
          setLoadingState(false);
          handleLoginResponse(body);
        });
      }
    });
  }

  /**
   * 验证输入
   */
  private boolean validateInput(String email, String password) {
    if (TextUtils.isEmpty(email)) {
      showMessage("请输入邮箱");
      etEmail.requestFocus();
      return false;
    }

    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      showMessage("请输入有效的邮箱地址");
      etEmail.requestFocus();
      return false;
    }

    if (TextUtils.isEmpty(password)) {
      showMessage("请输入密码");
      etPassword.requestFocus();
      return false;
    }

    if (password.length() < 6) {
      showMessage("密码长度不能少于6位");
      etPassword.requestFocus();
      return false;
    }

    return true;
  }

  /**
   * 设置加载状态
   */
  private void setLoadingState(boolean isLoading) {
    btnLogin.setEnabled(!isLoading);
    btnLogin.setText(isLoading ? "登录中..." : "登录");
    etEmail.setEnabled(!isLoading);
    etPassword.setEnabled(!isLoading);
  }

  /**
   * 处理登录响应
   */
  private void handleLoginResponse(String body) {
    try {
      LoginRes loginRes = gson.fromJson(body, LoginRes.class);

      if (loginRes.code == 200 && loginRes.data != null) {
        // 登录成功
        authManager.saveLoginResponse(loginRes.data);
        showMessage("登录成功");
        navigateToMain();
      } else {
        // 登录失败
        String errorMsg = !TextUtils.isEmpty(loginRes.msg) ? loginRes.msg : "登录失败";
        showMessage(errorMsg);
      }
    } catch (Exception e) {
      Log.e("LOGIN", "解析登录响应失败", e);
      showMessage("登录失败，请稍后重试");
    }
  }

  /**
   * 跳转到主页面
   */
  private void navigateToMain() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  /**
   * 跳转到注册页面
   */
  private void navigateToRegister() {
    Intent intent = new Intent(this, RegisterActivity.class);
    startActivity(intent);
  }

  /**
   * 跳转到忘记密码页面
   */
  private void navigateToForgotPassword() {
    Intent intent = new Intent(this, ForgotPasswordActivity.class);
    startActivity(intent);
  }

  /**
   * 显示消息
   */
  private void showMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }
}
