package com.mimeng.chess.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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

import com.google.gson.Gson;
import com.mimeng.chess.R;
import com.mimeng.chess.api.auth.AuthApi;
import com.mimeng.chess.api.auth.SendCodeReq;
import com.mimeng.chess.api.auth.SendCodeRes;
import com.mimeng.chess.api.auth.ResetPasswordReq;
import com.mimeng.chess.api.auth.ResetPasswordRes;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

/**
 * 忘记密码页面
 */
public class ForgotPasswordActivity extends AppCompatActivity {
  private EditText etEmail;
  private EditText etCode;
  private EditText etNewPassword;
  private Button btnSendCode;
  private Button btnResetPassword;
  private TextView tvBackToLogin;
  private ScrollView scrollView;

  private Gson gson;
  private CountDownTimer countDownTimer;
  private boolean isCodeSent = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // 删除标题栏
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_forgot_password);

    // 设置状态栏图标为深色，适配白色背景
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

    initViews();
    initData();
    setupListeners();
  }

  /**
   * 初始化视图
   */
  private void initViews() {
    etEmail = findViewById(R.id.et_email);
    etCode = findViewById(R.id.et_code);
    etNewPassword = findViewById(R.id.et_new_password);
    btnSendCode = findViewById(R.id.btn_send_code);
    btnResetPassword = findViewById(R.id.btn_reset_password);
    tvBackToLogin = findViewById(R.id.tv_back_to_login);
    scrollView = findViewById(R.id.scroll_view);
  }

  /**
   * 初始化数据
   */
  private void initData() {
    gson = new Gson();
  }

  /**
   * 设置监听器
   */
  private void setupListeners() {
    btnSendCode.setOnClickListener(v -> sendVerificationCode());
    btnResetPassword.setOnClickListener(v -> performResetPassword());
    tvBackToLogin.setOnClickListener(v -> navigateBackToLogin());

    // 设置输入框的IME监听器
    setupInputListeners();
  }

  /**
   * 设置输入框的软键盘监听器
   */
  private void setupInputListeners() {
    // 邮箱输入框按下"下一步"时发送验证码或移到验证码输入框
    etEmail.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_NEXT) {
        if (isCodeSent) {
          etCode.requestFocus();
        } else {
          sendVerificationCode();
        }
        return true;
      }
      return false;
    });

    // 验证码输入框按下"下一步"时焦点移到新密码输入框
    etCode.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_NEXT) {
        etNewPassword.requestFocus();
        return true;
      }
      return false;
    });

    // 新密码输入框按下"完成"时执行重置密码
    etNewPassword.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        hideKeyboard();
        performResetPassword();
        return true;
      }
      return false;
    });

    // 输入框获得焦点时滚动到可见位置
    etEmail.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });

    etCode.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });

    etNewPassword.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus) {
        v.postDelayed(() -> scrollToView(v), 300);
      }
    });
  }

  /**
   * 滚动到指定视图位置
   */
  private void scrollToView(View view) {
    scrollView.post(() -> {
      int[] location = new int[2];
      view.getLocationInWindow(location);
      int viewTop = location[1];

      int screenHeight = getResources().getDisplayMetrics().heightPixels;
      int keyboardHeight = screenHeight / 3;
      int visibleHeight = screenHeight - keyboardHeight;

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
   * 发送验证码
   */
  private void sendVerificationCode() {
    String email = etEmail.getText().toString().trim();

    // 验证邮箱
    if (!validateEmail(email)) {
      return;
    }

    // 设置发送验证码的加载状态
    setSendCodeLoadingState(true);

    // 调用发送验证码API
    SendCodeReq req = new SendCodeReq(email);
    AuthApi.instance.sendCode(req, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e("SEND_CODE", "发送验证码请求失败: " + e.getMessage(), e);
        runOnUiThread(() -> {
          setSendCodeLoadingState(false);
          showMessage("网络错误，请稍后重试");
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String body = response.body() != null ? response.body().string() : "";
        Log.d("SEND_CODE", "发送验证码响应: " + body);

        runOnUiThread(() -> {
          setSendCodeLoadingState(false);
          handleSendCodeResponse(body);
        });
      }
    });
  }

  /**
   * 执行重置密码
   */
  private void performResetPassword() {
    hideKeyboard();

    String email = etEmail.getText().toString().trim();
    String code = etCode.getText().toString().trim();
    String newPassword = etNewPassword.getText().toString().trim();

    // 输入验证
    if (!validateResetInput(email, code, newPassword)) {
      return;
    }

    // 设置加载状态
    setResetLoadingState(true);

    // 调用重置密码API
    ResetPasswordReq req = new ResetPasswordReq(email, code, newPassword);
    AuthApi.instance.resetPassword(req, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e("RESET_PASSWORD", "重置密码请求失败: " + e.getMessage(), e);
        runOnUiThread(() -> {
          setResetLoadingState(false);
          showMessage("网络错误，请稍后重试");
        });
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        String body = response.body() != null ? response.body().string() : "";
        Log.d("RESET_PASSWORD", "重置密码响应: " + body);

        runOnUiThread(() -> {
          setResetLoadingState(false);
          handleResetPasswordResponse(body);
        });
      }
    });
  }

  /**
   * 验证邮箱
   */
  private boolean validateEmail(String email) {
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

    return true;
  }

  /**
   * 验证重置密码输入
   */
  private boolean validateResetInput(String email, String code, String newPassword) {
    if (!validateEmail(email)) {
      return false;
    }

    if (TextUtils.isEmpty(code)) {
      showMessage("请输入验证码");
      etCode.requestFocus();
      return false;
    }

    if (TextUtils.isEmpty(newPassword)) {
      showMessage("请输入新密码");
      etNewPassword.requestFocus();
      return false;
    }

    if (newPassword.length() < 6) {
      showMessage("密码长度不能少于6位");
      etNewPassword.requestFocus();
      return false;
    }

    if (!isCodeSent) {
      showMessage("请先发送验证码");
      return false;
    }

    return true;
  }

  /**
   * 设置发送验证码加载状态
   */
  private void setSendCodeLoadingState(boolean isLoading) {
    btnSendCode.setEnabled(!isLoading);
    btnSendCode.setText(isLoading ? "发送中..." : "发送验证码");
    etEmail.setEnabled(!isLoading);
  }

  /**
   * 设置重置密码加载状态
   */
  private void setResetLoadingState(boolean isLoading) {
    btnResetPassword.setEnabled(!isLoading && isCodeSent);
    btnResetPassword.setText(isLoading ? "重置中..." : "重置密码");
    etCode.setEnabled(!isLoading && isCodeSent);
    etNewPassword.setEnabled(!isLoading);
  }

  /**
   * 处理发送验证码响应
   */
  private void handleSendCodeResponse(String body) {
    try {
      SendCodeRes sendCodeRes = gson.fromJson(body, SendCodeRes.class);

      if (sendCodeRes.code == 200) {
        // 发送成功
        isCodeSent = true;
        etCode.setEnabled(true);
        btnResetPassword.setEnabled(true);
        showMessage("验证码已发送，请查收邮箱");
        startCountDown();
      } else {
        // 发送失败
        String errorMsg = !TextUtils.isEmpty(sendCodeRes.msg) ? sendCodeRes.msg : "发送验证码失败";
        showMessage(errorMsg);
      }
    } catch (Exception e) {
      Log.e("SEND_CODE", "解析发送验证码响应失败", e);
      showMessage("发送验证码失败，请稍后重试");
    }
  }

  /**
   * 处理重置密码响应
   */
  private void handleResetPasswordResponse(String body) {
    try {
      ResetPasswordRes resetRes = gson.fromJson(body, ResetPasswordRes.class);

      if (resetRes.code == 200) {
        // 重置成功
        showMessage("密码重置成功，请使用新密码登录");
        navigateBackToLogin();
      } else {
        // 重置失败
        String errorMsg = !TextUtils.isEmpty(resetRes.msg) ? resetRes.msg : "密码重置失败";
        showMessage(errorMsg);
      }
    } catch (Exception e) {
      Log.e("RESET_PASSWORD", "解析重置密码响应失败", e);
      showMessage("密码重置失败，请稍后重试");
    }
  }

  /**
   * 开始倒计时
   */
  private void startCountDown() {
    countDownTimer = new CountDownTimer(60000, 1000) {
      @Override
      public void onTick(long millisUntilFinished) {
        btnSendCode.setEnabled(false);
        btnSendCode.setText("重新发送(" + millisUntilFinished / 1000 + "s)");
      }

      @Override
      public void onFinish() {
        btnSendCode.setEnabled(true);
        btnSendCode.setText("重新发送");
      }
    }.start();
  }

  /**
   * 返回登录页面
   */
  private void navigateBackToLogin() {
    finish(); // 关闭忘记密码页面，返回登录页面
  }

  /**
   * 显示消息
   */
  private void showMessage(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (countDownTimer != null) {
      countDownTimer.cancel();
    }
  }
}
